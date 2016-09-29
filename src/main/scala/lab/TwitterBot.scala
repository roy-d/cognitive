package lab

import akka.actor.{Actor, ActorLogging, Props}
import lab.Conversation.{ConversationConfig, ConversationResponse}
import lab.NLClassifier.{NLClassifierConfig, NLClassifierResponse}
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
  val nlClassifier = context.actorOf(NLClassifier.props(twitterBotConfig.nlClassifierConf), "nlClassifier")
  val conversation = context.actorOf(Conversation.props(twitterBotConfig.converseConf), "conversation")

  val auth = new OAuthAuthorization(twitterBotConfig.twitterConf)
  val twitter = new TwitterFactory().getInstance(auth)

  def receive = {
    case payload: Payload =>
      log.info(s"<<< ${payload.user} |${payload.tweet}|")
      conversation ! payload

    case converseResponse: ConversationResponse =>
      log.info(s">>> ${converseResponse.payload.user}|${converseResponse.entities}|${converseResponse.intent}|")
      val payload = converseResponse.payload
      if (payload.tweet.contains("analyze: ") || converseResponse.intent.contains("analytics")) {
        toneAnalyzer ! payload
        textAnalyzer ! payload
        nlClassifier ! payload

        val page = new Paging(1, 200)
        val history = twitter.getUserTimeline(payload.user, page).toList.map(
          status =>
            status.getText
        ).mkString(". ")
        personalityAnalyzer ! Payload(payload.user, payload.tweet, payload.id, Some(history))
        tweet(s"@${payload.user} following powered by @IBMWatson", payload.id)
      } else if (converseResponse.intent contains "greetings") {
        tweet(s"Hi @${payload.user} ! How may i help you today (Intent:${converseResponse.intent})", payload.id)
      } else if (converseResponse.intent contains "compliments") {
        tweet(s"It was such a pleasure to help you @${payload.user} (Intent:${converseResponse.intent})", payload.id)
      } else if (converseResponse.intent contains "capabilities") {
        tweet(s"@${payload.user} I can analyze your tweet. Just ask me to `analyze: <tweet>` (Intent:${converseResponse.intent})", payload.id)
      } else if (converseResponse.intent contains "aboutme") {
        tweet(s"@${payload.user} https://github.com/roy-d/cognitive (Intent:${converseResponse.intent})", payload.id)
      } else if (converseResponse.intent contains "goodbye") {
        tweet(s"@${payload.user} have a wonderful day ! (Intent:${converseResponse.intent})", payload.id)
      } else {
        tweet(s"@${payload.user} I am not sure of: (Intent:${converseResponse.intent}) (Entities:${converseResponse.entities})", payload.id)
      }

    case textAnalyticsResponse: TextAnalyticsResponse =>
      log.info(s">>> ${textAnalyticsResponse.payload.user}|${textAnalyticsResponse.response}|")
      tweet(s"@${textAnalyticsResponse.payload.user}: ${textAnalyticsResponse.response}", textAnalyticsResponse.payload.id)

    case toneAnalyticsResponse: ToneAnalyticsResponse =>
      log.info(s">>> ${toneAnalyticsResponse.payload.user}|${toneAnalyticsResponse.response}|")
      tweet(s"@${toneAnalyticsResponse.payload.user}: Tone=${toneAnalyticsResponse.response}", toneAnalyticsResponse.payload.id)

    case nlClassifierResponse: NLClassifierResponse =>
      log.info(s">>> ${nlClassifierResponse.payload.user}|${nlClassifierResponse.response}|")
      tweet(s"@${nlClassifierResponse.payload.user}: NLC=${nlClassifierResponse.response}", nlClassifierResponse.payload.id)

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
                               personalityAnalyticsConf: PersonalityAnalyticsConfig,
                               nlClassifierConf: NLClassifierConfig,
                               converseConf: ConversationConfig
                             )

  case class Payload(user: String, tweet: String, id: String, oldTweets: Option[String])

  def props(posterConfig: TwitterBotConfig): Props = Props(new TwitterBot(posterConfig))
}
