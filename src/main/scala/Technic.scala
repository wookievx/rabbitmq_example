import akka.actor.{ActorSystem, Props}
import com.spingo.op_rabbit.PlayJsonSupport._
import com.spingo.op_rabbit._

import scala.util.Random

class Technic(actorSystem: ActorSystem, connectionParams: ConnectionParams, taskTypes: List[String]) {

  import Model._

  private implicit val recoveryStrategy = RecoveryStrategy.none
  import actorSystem.dispatcher

  private val rabbitControl = actorSystem.actorOf(Props {
    new RabbitControl(connectionParams)
  })
  private val defaultExchange = Exchange.topic(RequestExchange, durable = false)

  private val adminSubscription = Administrator.adminChannel("Technik", s"tech.${Random.nextInt(1000)}", rabbitControl, defaultExchange)

  val subscriptionRefs: List[SubscriptionRef] = taskTypes.map { taskType =>
      Subscription.run(rabbitControl) {
        import Directives._
        channel(qos = 1) {
          consume(topic(queue(s"technic.$taskType", durable = false), topics = List(taskType).map(typeToTopic), exchange = defaultExchange)) {
            body(as[Task]) {
              case t@Task(_, id, _) =>
                rabbitControl ! Message.topic(Response(t, Random.nextBoolean()), routingKey = idToTopic(id), exchange = RequestExchange)
                ack
            }
          }
        }
      }
  } :+ adminSubscription


}
