package lab

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import lab.NLClassifier.NLClassifierConfig
import lab.PersonalityAnalytics.PersonalityAnalyticsConfig
import lab.TextAnalytics.TextAnalyticsConfig
import lab.ToneAnalytics.ToneAnalyticsConfig
import lab.TwitterBot.{Payload, TwitterBotConfig}
import twitter4j._
import twitter4j.conf.ConfigurationBuilder

object Server extends App {
  val config = ConfigFactory.load()
  val hashTag = config.getString("cognitive.twitter.hashTag")

  val configurationBuilder = new ConfigurationBuilder()
  configurationBuilder.setOAuthConsumerKey(config.getString("cognitive.twitter.consumerKey"))
    .setOAuthConsumerSecret(config.getString("cognitive.twitter.consumerSecret"))
    .setOAuthAccessToken(config.getString("cognitive.twitter.accessToken"))
    .setOAuthAccessTokenSecret(config.getString("cognitive.twitter.accessTokenSecret"))

  val twitterConf = configurationBuilder.build()
  val twitterStream = new TwitterStreamFactory(twitterConf).getInstance()

  val toneAnalyticsConfig = ToneAnalyticsConfig(config.getString("cognitive.watson.tone.username"), config.getString("cognitive.watson.tone.password"))
  val textAnalyticsConfig = TextAnalyticsConfig(config.getString("cognitive.watson.alchemy.apikey"))
  val personalityAnalyticsConfig = PersonalityAnalyticsConfig(config.getString("cognitive.watson.personality.username"), config.getString("cognitive.watson.personality.password"))
  val nlClassifierConfig = NLClassifierConfig(config.getString("cognitive.watson.nlc.username"), config.getString("cognitive.watson.nlc.password"))

  val system = ActorSystem("CognitiveSystem")
  val twitterBot = system.actorOf(TwitterBot.props(
    TwitterBotConfig(twitterConf, toneAnalyticsConfig, textAnalyticsConfig, personalityAnalyticsConfig, nlClassifierConfig)
  ), "twitterBot")

  twitterStream.addListener(new StatusListener() {
    override def onStatus(status: Status) =
      twitterBot ! Payload(status.getUser.getScreenName, status.getText.replace(hashTag, ""), status.getId.toString.takeRight(8), None)

    override def onStallWarning(warning: StallWarning): Unit = sys.error(warning.toString)

    override def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice): Unit = println(statusDeletionNotice)

    override def onScrubGeo(userId: Long, upToStatusId: Long): Unit = println(userId)

    override def onTrackLimitationNotice(numberOfLimitedStatuses: Int): Unit = println(numberOfLimitedStatuses)

    override def onException(ex: Exception): Unit = ex.printStackTrace()
  })

  val tweetFilterQuery = new FilterQuery()
  tweetFilterQuery.track(hashTag)
  tweetFilterQuery.language("en")

  twitterStream.filter(tweetFilterQuery)
}
