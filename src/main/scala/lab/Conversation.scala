package lab

import akka.actor.{Actor, ActorLogging, Props}
import com.ibm.watson.developer_cloud.conversation.v1.ConversationService
import com.ibm.watson.developer_cloud.conversation.v1.model.MessageRequest
import lab.Conversation.{ConversationConfig, ConversationResponse}
import lab.TwitterBot.Payload

import scala.collection.JavaConversions._

class Conversation(converseConf: ConversationConfig) extends Actor with ActorLogging {
  val service = new ConversationService(ConversationService.VERSION_DATE_2016_07_11)
  service.setUsernameAndPassword(converseConf.userName, converseConf.password)
  val builder = new MessageRequest.Builder()

  def receive = {
    case payload: Payload =>
      val newMessage = builder.inputText(payload.tweet).build()
      val response = service.message(converseConf.workspace, newMessage).execute()
      val intent = response.getIntents.toList.sortWith(_.getConfidence > _.getConfidence).head
      val entities = response.getEntities.toList.map(_.getEntity).toString().replace("List","")
      val intentSummary = f"${intent.getIntent}%s(${intent.getConfidence}%1.1f)"

      sender() ! ConversationResponse(entities, intentSummary, payload)
  }
}

object Conversation {

  case class ConversationConfig(userName: String, password: String, workspace: String)

  case class ConversationResponse(entities: String, intent: String, payload: Payload)

  def props(converseConf: ConversationConfig): Props = Props(new Conversation(converseConf))
}
