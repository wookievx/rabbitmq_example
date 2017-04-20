import akka.actor.ActorSystem
import com.rabbitmq.client.{Address, ConnectionFactory}
import com.spingo.op_rabbit.ConnectionParams

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{Failure, Success}

object DefaultApp extends App {

  import Model._

  val connectionParams = ConnectionParams(
    hosts = List(new Address("localhost", ConnectionFactory.DEFAULT_AMQP_PORT)),
    username = "guest",
    password = "guest"
  )

  val actorSystem = ActorSystem("main-system")

  import actorSystem.{dispatcher, scheduler}

  val technics = types.permutations.collect {
    case f :: s :: _ => new Technic(actorSystem, connectionParams, List(f, s))
  }

  val initializationFuture = Future.sequence(technics.flatMap(_.subscriptionRefs).map(_.initialized)).flatMap { _ =>
    val doctors = (1 to 3).map(t => new Doctor(actorSystem, connectionParams, s"doctor_$t"))
    Future.sequence(doctors.flatMap(_.subscriptionRefs).map(_.initialized)).map(_ => doctors)
  }

  initializationFuture.onComplete {
    case Success(doctors) =>
      println("INITIALIZED")
      val subscriptions = technics.flatMap(_.subscriptionRefs) ++ doctors.flatMap(_.subscriptionRefs)
      scheduler.scheduleOnce(1 minute) {
        subscriptions.foreach(_.close(10 seconds))
        scheduler.scheduleOnce(1 minute) {
          actorSystem.terminate()
        }
      }
      val admin = new Administrator(actorSystem, connectionParams)
      admin.subscriptions.foreach(s => s.initialized.onSuccess {
        case _ =>
          scheduler.scheduleOnce(1 minute) {
            s.close(10 seconds)
          }
      })
    case Failure(r) =>
      println(s"Failed to initialize system: ${r.getMessage}")
  }

}
