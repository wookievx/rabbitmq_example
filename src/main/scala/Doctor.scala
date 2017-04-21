import Model._
import akka.actor.{ActorSystem, Props}
import akka.util.Timeout
import com.spingo.op_rabbit.PlayJsonSupport._
import com.spingo.op_rabbit._

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class Doctor(actorSystem: ActorSystem, connectionParams: ConnectionParams, id: String) {

  private implicit val timeout = Timeout(5 seconds)
  private implicit val recoveryStrategy = RecoveryStrategy.none

  import actorSystem.{dispatcher, scheduler}

  private val control = actorSystem.actorOf(Props {
    new RabbitControl(connectionParams)
  })

  private val exchangeDefault = Exchange.topic(RequestExchange, durable = false)

  private val adminSubscription = Administrator.adminChannel(id, s"$id.adm", control, exchangeDefault)

  val subscriptionRefs: List[SubscriptionRef] = Subscription.run(control) {
    import Directives._
    channel(qos = 1) {
      consume(topic(queue(id, durable = false, autoDelete = true), topics = List(id).map(idToTopic), exchange = exchangeDefault)) {
        body(as[Response]) {
          case Response(Task(taskType, rid, name), healthy) =>
            ack(Future {
              println(s"[$id]: Is $name $taskType healthy? $healthy")
            })
        }
      }
    }
  } :: adminSubscription :: Nil

  scheduler.schedule(1 milli, (Random.nextInt(2000) + 1500) millis ) {
    val randomType = Random.shuffle(types).head
    control ! Message.exchange(Task(randomType, id, "patient0"), RequestExchange, typeToTopic(randomType))
  }


}
