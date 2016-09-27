package lab

import akka.actor.{Actor, ActorLogging, Props}
import lab.Poster.Payload
import twitter4j.TwitterFactory
import twitter4j.auth.OAuthAuthorization
import twitter4j.conf.Configuration

class Poster(twitterConf: Configuration) extends Actor with ActorLogging {
  val auth = new OAuthAuthorization(twitterConf)
  val twitter = new TwitterFactory().getInstance(auth)

  def receive = {
    case payload: Payload => twitter.updateStatus(s"@${payload.user} : ${payload.tweet}")
  }
}

object Poster {

  case class Payload(user: String, tweet: String)

  def props(twitterConf: Configuration): Props = Props(new Poster(twitterConf))
}
