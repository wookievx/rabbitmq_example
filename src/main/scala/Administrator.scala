import java.util.Date

import akka.actor.{ActorSystem, Props}
import com.spingo.op_rabbit._

import scala.concurrent.Future
import scala.util.{Random, Success}

class Administrator(actorSystem: ActorSystem, connectionParams: ConnectionParams) {
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

  Future {
    while (true) {
      val command = io.StdIn.readLine("Message:\n")
      rabbitControl ! Message.topic(AdminMessage(new Date, command), AdminExchange)
    }
  }

}
