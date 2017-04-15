import Model._
import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import com.spingo.op_rabbit._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

class Doctor(actorSystem: ActorSystem, connectionParams: ConnectionParams) {

  private implicit val timeout = Timeout(5 seconds)
  private implicit val recoveryStrategy = RecoveryStrategy.none

  import actorSystem.{dispatcher, scheduler}

  private val control = actorSystem.actorOf(Props {new RabbitControl(connectionParams) })
  scheduler.schedule(1 milli, 1 second) {
    val randomType = Random.shuffle(types).head
    val response = (control ? Message.topic(Task(randomType, "TestPatient"), technicExchange(randomType))).mapTo[Response]
    response.onComplete {
      case Success(Response(Task(t, n), healthy)) =>
        print(s"$n's $t is ")
        if (healthy) println("healthy") else println("sick")
      case Failure(reason) =>
        println(s"Response unavailable due to: ${reason.getMessage}")
    }
  }



}
