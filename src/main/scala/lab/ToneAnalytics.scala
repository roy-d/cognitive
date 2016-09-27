package lab

import akka.actor.{Actor, ActorLogging, Props}
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer
import lab.ToneAnalytics.{ToneAnalyticsConfig, ToneAnalyticsResponse}
import lab.TwitterBot.Payload

import scala.collection.JavaConversions._

class ToneAnalytics(toneAnalyticsConf: ToneAnalyticsConfig) extends Actor with ActorLogging {
  val service = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_05_19)
  service.setUsernameAndPassword(toneAnalyticsConf.userName, toneAnalyticsConf.password)

  def receive = {
    case payload: Payload =>
      val tone = service.getTone(payload.tweet, null).execute()

      log.debug(tone.toString)

      val maxTone = tone.getDocumentTone.getTones.toList
        .map(category => (category.getName.replace(" Tone", ""), category.getTones.toList.sortWith(_.getScore > _.getScore).head))
        .filter { case (_, v) => v.getScore > 0 }
        .toMap
        .mapValues(tone => f"${tone.getName}%s(${tone.getScore}%1.1f)")

      sender() ! ToneAnalyticsResponse(maxTone.toString.replace("Map", "").replace(" -> ", ":"), payload)
  }
}

object ToneAnalytics {

  case class ToneAnalyticsConfig(userName: String, password: String)

  case class ToneAnalyticsResponse(response: String, payload: Payload)

  def props(tonerConf: ToneAnalyticsConfig): Props = Props(new ToneAnalytics(tonerConf))
}
