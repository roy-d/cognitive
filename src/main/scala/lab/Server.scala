package lab

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import lab.TextAnalytics.TextAnalyticsConfig
import lab.TwitterBot.{Payload, TwitterBotConfig}
import lab.ToneAnalytics.ToneAnalyticsConfig
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
  val tonerConfig = ToneAnalyticsConfig(config.getString("cognitive.watson.tone.username"), config.getString("cognitive.watson.tone.password"))
  val alchemyConfig = TextAnalyticsConfig(config.getString("cognitive.watson.alchemy.apikey"))
  val system = ActorSystem("CognitiveSystem")
  val twitterBot = system.actorOf(TwitterBot.props(TwitterBotConfig(twitterConf, tonerConfig, alchemyConfig)), "twitterBot")

  twitterStream.addListener(new StatusListener() {
    override def onStatus(status: Status) =
      twitterBot ! Payload(status.getUser.getScreenName, status.getText.replace(hashTag, ""), status.getId.toString.takeRight(8))

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
