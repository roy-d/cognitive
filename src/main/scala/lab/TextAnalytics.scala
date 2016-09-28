package lab

import java.util

import akka.actor.{Actor, ActorLogging, Props}
import com.ibm.watson.developer_cloud.alchemy.v1.AlchemyLanguage
import lab.TextAnalytics.{TextAnalyticsConfig, TextAnalyticsResponse}
import lab.TwitterBot.Payload

import scala.collection.JavaConversions._
import scala.util.Try

class TextAnalytics(textAnalyticsConf: TextAnalyticsConfig) extends Actor with ActorLogging {
  val service = new AlchemyLanguage()
  service.setApiKey(textAnalyticsConf.apikey)

  def receive = {
    case payload: Payload =>
      val params = new util.HashMap[String, Object]()
      params.put(AlchemyLanguage.TEXT, payload.tweet)

      val sentiment = service.getSentiment(params).execute()
      val sentimentSummary = f"${sentiment.getSentiment.getType.name()}%s(${Try(sentiment.getSentiment.getScore.doubleValue).getOrElse(0.0d)}%1.1f)"

      val entities = service.getEntities(params).execute()
      val entitySummary = entities.getEntities.toList.map(
        entity =>
          entity.getText ->(entity.getType, Try(entity.getDisambiguated.getSubType.toList.take(3)).getOrElse(""))
      ).toMap.toString.replace("Map", "").replace(" -> ", ":")

      val relations = service.getTypedRelations(params).execute()
      val relationSummary = relations.getTypedRelations.toList.map(
        relation =>
          relation.getType -> relation.getScore
      ).toMap.toString.replace("Map", "").replace(" -> ", ":")

      sender() ! TextAnalyticsResponse(s"Entities=$entitySummary, Relations=$relationSummary, Sentiment=$sentimentSummary", payload)
  }
}

object TextAnalytics {

  case class TextAnalyticsConfig(apikey: String)

  case class TextAnalyticsResponse(response: String, payload: Payload)

  def props(textAnalyticsConf: TextAnalyticsConfig): Props = Props(new TextAnalytics(textAnalyticsConf))
}
