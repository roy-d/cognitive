package lab

import akka.actor.{Actor, ActorLogging, Props}
import lab.PersonalityAnalytics.{PersonalityAnalyticsConfig, PersonalityAnalyticsResponse}
import lab.TextAnalytics.{TextAnalyticsConfig, TextAnalyticsResponse}
import lab.ToneAnalytics.{ToneAnalyticsConfig, ToneAnalyticsResponse}
import lab.TwitterBot.{Payload, TwitterBotConfig}
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.Configuration
import twitter4j.{Paging, TwitterFactory}

import scala.annotation.tailrec
import scala.collection.JavaConversions._

class TwitterBot(twitterBotConfig: TwitterBotConfig) extends Actor with ActorLogging {
  val toneAnalyzer = context.actorOf(ToneAnalytics.props(twitterBotConfig.toneAnalyticsConf), "toneAnalyzer")
  val textAnalyzer = context.actorOf(TextAnalytics.props(twitterBotConfig.textAnalyticsConf), "textAnalyzer")
  val personalityAnalyzer = context.actorOf(PersonalityAnalytics.props(twitterBotConfig.personalityAnalyticsConf), "personalityAnalyzer")

  val auth = new OAuthAuthorization(twitterBotConfig.twitterConf)
  val twitter = new TwitterFactory().getInstance(auth)

  def receive = {
    case payload: Payload =>
      log.info(s"<<< ${payload.user} |${payload.tweet}|")
      toneAnalyzer ! payload
      textAnalyzer ! payload
      val page = new Paging(1, 200)
      val history = twitter.getUserTimeline(payload.user, page).toList.map(
        status =>
          status.getText
      ).mkString(". ")
      personalityAnalyzer ! Payload(payload.user, payload.tweet, payload.id, Some(history))
      tweet(s"@${payload.user} following powered by @IBMWatson", payload.id)

    case textAnalyticsResponse: TextAnalyticsResponse =>
      log.info(s">>> ${textAnalyticsResponse.payload.user}|${textAnalyticsResponse.response}|")
      tweet(s"@${textAnalyticsResponse.payload.user}: ${textAnalyticsResponse.response}", textAnalyticsResponse.payload.id)

    case toneAnalyticsResponse: ToneAnalyticsResponse =>
      log.info(s">>> ${toneAnalyticsResponse.payload.user}|${toneAnalyticsResponse.response}|")
      tweet(s"@${toneAnalyticsResponse.payload.user}: Tone=${toneAnalyticsResponse.response}", toneAnalyticsResponse.payload.id)

    case personalityAnalyticsResponse: PersonalityAnalyticsResponse =>
      log.info(s">>> ${personalityAnalyticsResponse.payload.user}|${personalityAnalyticsResponse.response}|")
      tweet(s"@${personalityAnalyticsResponse.payload.user}: Personality=${personalityAnalyticsResponse.response}", personalityAnalyticsResponse.payload.id)

  }

  def tweet(status: String, id: String): Unit = {
    val idString = s" [$id]"
    val threshold = 140 - idString.length
    @tailrec
    def helper(remainder: String): Unit = {
      if (remainder.length <= threshold) twitter.updateStatus(s"$remainder$idString")
      else {
        val (first, second) = remainder.splitAt(threshold)
        twitter.updateStatus(s"$first$idString")
        helper(second)
      }
    }
    helper(status)
  }
}

object TwitterBot {

  case class TwitterBotConfig(
                               twitterConf: Configuration,
                               toneAnalyticsConf: ToneAnalyticsConfig,
                               textAnalyticsConf: TextAnalyticsConfig,
                               personalityAnalyticsConf: PersonalityAnalyticsConfig
                             )

  case class Payload(user: String, tweet: String, id: String, oldTweets: Option[String])

  def props(posterConfig: TwitterBotConfig): Props = Props(new TwitterBot(posterConfig))
}
