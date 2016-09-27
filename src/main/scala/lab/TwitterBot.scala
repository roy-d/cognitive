package lab

import akka.actor.{Actor, ActorLogging, Props}
import lab.TextAnalytics.{TextAnalyticsConfig, TextAnalyticsResponse}
import lab.TwitterBot.{Payload, TwitterBotConfig}
import lab.ToneAnalytics.{ToneAnalyticsConfig, ToneAnalyticsResponse}
import twitter4j.TwitterFactory
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.Configuration

class TwitterBot(posterConfig: TwitterBotConfig) extends Actor with ActorLogging {
  val toneAnalyzer = context.actorOf(ToneAnalytics.props(posterConfig.tonerConf), "toneAnalyzer")
  val textAnalyzer = context.actorOf(TextAnalytics.props(posterConfig.alchemyConf), "textAnalyzer")

  val auth = new OAuthAuthorization(posterConfig.twitterConf)
  val twitter = new TwitterFactory().getInstance(auth)

  def receive = {
    case payload: Payload =>
      log.info(s"<<< ${payload.user} |${payload.tweet}|")
      toneAnalyzer ! payload
      textAnalyzer ! payload
      twitter.updateStatus(s"@${payload.user} My best buddy @IBMWatson says following, for ID: [${payload.id}]")

    case textAnalyticsResponse: TextAnalyticsResponse =>
      log.info(s">>> ${textAnalyticsResponse.payload.user} |${textAnalyticsResponse.toString}|")
      twitter.updateStatus(s"@${textAnalyticsResponse.payload.user}: ${textAnalyticsResponse.response} [${textAnalyticsResponse.payload.id}]")

    case toneAnalyticsResponse: ToneAnalyticsResponse =>
      log.info(s">>> ${toneAnalyticsResponse.payload.user} |${toneAnalyticsResponse.toString}|")
      twitter.updateStatus(s"@${toneAnalyticsResponse.payload.user}: Tone=${toneAnalyticsResponse.response} [${toneAnalyticsResponse.payload.id}]")

  }
}

object TwitterBot {

  case class TwitterBotConfig(twitterConf: Configuration, tonerConf: ToneAnalyticsConfig, alchemyConf: TextAnalyticsConfig)

  case class Payload(user: String, tweet: String, id: String)

  def props(posterConfig: TwitterBotConfig): Props = Props(new TwitterBot(posterConfig))
}
