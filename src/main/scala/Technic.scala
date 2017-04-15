import akka.actor.{ActorSystem, Props}
import com.spingo.op_rabbit._

import scala.util.{Random, Success}

class Technic(actorSystem: ActorSystem, connectionParams: ConnectionParams) {
  import Model._
  private implicit val recoveryStrategy = RecoveryStrategy.none

  import actorSystem.dispatcher

  private val rabbitControl = actorSystem.actorOf(Props[RabbitControl])
  private val subscriptionRef = Subscription.run(rabbitControl) {
    import Directives._
    channel(qos = 1) {
      consume(topic(queue(DefaultQueue), topics = List("technic.#"))) {
        (body(as[Task]) & routingKey) { (task, key) =>
          println(s"Received task for $key")
          ack(Success(Response(task, Random.nextBoolean())))
        }
      }
    }
  }


}
