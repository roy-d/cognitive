package lab

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import lab.Poster.Payload
import twitter4j._
import twitter4j.conf.ConfigurationBuilder

object Server extends App {

  val hashTag = "#matroyd"

  val config = ConfigFactory.load()

  val configurationBuilder = new ConfigurationBuilder()

  configurationBuilder.setOAuthConsumerKey(config.getString("cognitive.twitter.consumerKey"))
    .setOAuthConsumerSecret(config.getString("cognitive.twitter.consumerSecret"))
    .setOAuthAccessToken(config.getString("cognitive.twitter.accessToken"))
    .setOAuthAccessTokenSecret(config.getString("cognitive.twitter.accessTokenSecret"))

  val twitterConf = configurationBuilder.build()

  val twitterStream = new TwitterStreamFactory(twitterConf).getInstance()

  val system = ActorSystem("CognitiveSystem")
  val poster = system.actorOf(Poster.props(twitterConf), "poster")

  twitterStream.addListener(new StatusListener() {
    override def onStatus(status: Status) = {
      println(s"!!! ${status.getUser.getScreenName} sent: ${status.getText}")
      poster ! Payload(status.getUser.getScreenName, status.getText.replace(hashTag, ""))
    }

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
