import java.util.Date

import com.spingo.op_rabbit.PlayJsonSupport._
import play.api.libs.json._

object Model {
  val DefaultQueue = "test-queue"
  case class Task(taskType: String, name: String)
  case class Response(task: Task, healthy: Boolean)
  case class AdminMessage(date: Date, msg: String)
  implicit val personFormat: OFormat[Task] = Json.format[Task]
  implicit val responseFormat: OFormat[Response] = Json.format[Response]
  implicit val messageFormat: OFormat[AdminMessage] = Json.format[AdminMessage]

  def technicExchange(exchangeType: String) = s"process.$exchangeType"
  val AdminExchange = "admin"

  val Knee = "knee"
  val Ankle = "ankle"
  val Elbow = "elbow"
  val types = List(Knee, Ankle, Elbow)

}
