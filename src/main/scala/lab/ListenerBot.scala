package lab

import java.util.UUID

import akka.actor.{Actor, ActorLogging, Props}
import lab.ListenerBot.ListenerBotConfig
import lab.TextAnalytics.{TextAnalyticsConfig, TextAnalyticsResponse}
import lab.ToneAnalytics.{ToneAnalyticsConfig, ToneAnalyticsResponse}
import lab.TwitterBot.Payload

class ListenerBot(listenerConfig: ListenerBotConfig) extends Actor with ActorLogging {
  val toneAnalyzer = context.actorOf(ToneAnalytics.props(listenerConfig.toneAnalyticsConf), "toneAnalyzer")
  val textAnalyzer = context.actorOf(TextAnalytics.props(listenerConfig.textAnalyticsConf), "textAnalyzer")

  def receive = {
    case sentence: String =>
      println(s"\n$sentence")
      val payload = Payload("ListenerBot", sentence, UUID.randomUUID().toString, None)
      toneAnalyzer ! payload
      textAnalyzer ! payload

    case textAnalyticsResponse: TextAnalyticsResponse =>
      println(s"|__\t\t${textAnalyticsResponse.response}")

    case toneAnalyticsResponse: ToneAnalyticsResponse =>
      println(s"|__\t\t${toneAnalyticsResponse.response}")
  }
}

object ListenerBot {

  case class ListenerBotConfig(
                                toneAnalyticsConf: ToneAnalyticsConfig,
                                textAnalyticsConf: TextAnalyticsConfig
                              )

  def props(listenerConfig: ListenerBotConfig): Props = Props(new ListenerBot(listenerConfig))
}

