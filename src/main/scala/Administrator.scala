import java.util.Date

import Model.AdminMessage
import akka.actor.{ActorRef, ActorSystem, Props}
import com.spingo.op_rabbit.PlayJsonSupport._
import com.spingo.op_rabbit._

import scala.collection.concurrent.TrieMap
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.language.postfixOps

class Administrator(actorSystem: ActorSystem, connectionParams: ConnectionParams) {
  import Model._
  private implicit val recoveryStrategy = RecoveryStrategy.none
  import actorSystem.{dispatcher, scheduler}

  private val statsMap = TrieMap.empty[String, Int].withDefaultValue(0)
  private val responseMap = TrieMap.empty[String, Int].withDefaultValue(0)

  private val rabbitControl = actorSystem.actorOf(Props{ new RabbitControl(connectionParams)})
  private val requestExchange = Exchange.topic(RequestExchange, durable = false)

  def stringUnmarshalerDupa = new RabbitUnmarshaller[String] {
    override def unmarshall(value: Array[Byte], contentType: Option[String], contentEncoding: Option[String]): String = new String(value)
  }

  val requestSubscriptionRef: SubscriptionRef = Subscription.run(rabbitControl) {
    import Directives._
    channel(qos = 1) {
      consume(topic(queue("AdminQ", durable = false), topics = List(s"$TaskPrefix.#"), exchange = requestExchange)) {
        (body(as[Task]) & routingKey) { (task, rk) =>
          statsMap(task.taskType) += 1
          ack
        }
      }
    }
  }



  val responseSubscriptionRef: SubscriptionRef = Subscription.run(rabbitControl) {
    import Directives._
    channel(qos = 1) {
      consume(topic(queue("AdminQ2", durable = false), topics = List(idToTopic("#")), exchange = requestExchange)) {
        (body(as[Response]) & routingKey) { (response, rk) =>
          responseMap(response.task.id) += 1
          ack
        }
      }
    }
  }

  val subscriptions = List(requestSubscriptionRef, responseSubscriptionRef)

  scheduler.schedule(5 seconds, 10 seconds) {
    println(s"Current statistics:\n${statsMap.map{ case (t, numb) => s"$t : $numb"}.mkString("\n")}")
    println(responseMap.map{ case (t, numb) => s"$t : $numb"}.mkString("\n"))
    rabbitControl ! Message.topic(AdminMessage(new Date(), s"All request processed: ${statsMap.values.sum}"), routingKey = "admin.msg", exchange = RequestExchange)
  }

}

object Administrator {

  def adminChannel(displayName: String, queueName: String, control: ActorRef, requestExchange: Exchange[Exchange.Topic.type])(implicit ev: RecoveryStrategy, ctx: ExecutionContext): SubscriptionRef = Subscription.run(control) {
    import Directives._
    channel(qos = 1) {
      consume(topic(queue(queueName, durable = false, autoDelete = true), topics = List("admin.msg"), exchange = requestExchange)) {
        (body(as[AdminMessage]) & routingKey) { case (AdminMessage(date, msg), rk) =>
          println(s"[$displayName, $date] INFO: $msg")
          ack
        }
      }
    }
  }

}
