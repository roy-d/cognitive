package lab

import akka.actor.{Actor, ActorLogging, Props}
import com.ibm.watson.developer_cloud.personality_insights.v2.PersonalityInsights
import lab.PersonalityAnalytics.{PersonalityAnalyticsConfig, PersonalityAnalyticsResponse}
import lab.TwitterBot.Payload

import scala.collection.JavaConversions._

class PersonalityAnalytics(personalityAnalyticsConf: PersonalityAnalyticsConfig) extends Actor with ActorLogging {
  val service = new PersonalityInsights()
  service.setUsernameAndPassword(personalityAnalyticsConf.userName, personalityAnalyticsConf.password)

  def receive = {
    case payload: Payload =>
      val profile = service.getProfile(payload.oldTweets.get).execute()

      val profileSummary = profile.getTree.getChildren.toList.map(
        child => child.getId -> child.getChildren.toList.sortWith(_.getPercentage > _.getPercentage).head)
        .toMap
        .mapValues(child => child.getChildren.toList.sortWith(_.getPercentage > _.getPercentage).take(3).map(
          gChild => (gChild.getId, f"${gChild.getPercentage}%1.1f")
        ))
        .toString
        .replace("Map", "")
        .replace("List", "")
        .replace(" ", "")
        .replace(" -> ", ":")

      sender() ! PersonalityAnalyticsResponse(profileSummary, payload)
  }
}

object PersonalityAnalytics {

  case class PersonalityAnalyticsConfig(userName: String, password: String)

  case class PersonalityAnalyticsResponse(response: String, payload: Payload)

  def props(personalityAnalyticsConf: PersonalityAnalyticsConfig): Props = Props(new PersonalityAnalytics(personalityAnalyticsConf))
}
