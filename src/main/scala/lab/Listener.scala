package lab

import javax.sound.sampled._

import akka.actor.ActorSystem
import com.ibm.watson.developer_cloud.http.HttpMediaType
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.{RecognizeOptions, SpeechResults}
import com.ibm.watson.developer_cloud.speech_to_text.v1.websocket.BaseRecognizeCallback
import com.typesafe.config.ConfigFactory
import lab.ListenerBot.ListenerBotConfig
import lab.TextAnalytics.TextAnalyticsConfig
import lab.ToneAnalytics.ToneAnalyticsConfig

import scala.collection.JavaConversions._

object Listener extends App {

  val config = ConfigFactory.load()
  val service = new SpeechToText()
  service.setUsernameAndPassword(
    config.getString("cognitive.watson.speech.username"),
    config.getString("cognitive.watson.speech.password")
  )

  val toneAnalyticsConfig = ToneAnalyticsConfig(config.getString("cognitive.watson.tone.username"), config.getString("cognitive.watson.tone.password"))
  val textAnalyticsConfig = TextAnalyticsConfig(config.getString("cognitive.watson.alchemy.apikey"))
  val listenerBotConfig = ListenerBotConfig(toneAnalyticsConfig, textAnalyticsConfig)

  val system = ActorSystem("ListenerSystem")
  val listenerBot = system.actorOf(ListenerBot.props(listenerBotConfig), "listenerBot")

  val sampleRate = 16000
  val format = new AudioFormat(sampleRate, 16, 1, true, false)
  val info = new DataLine.Info(classOf[TargetDataLine], format)

  if (!AudioSystem.isLineSupported(info)) {
    println(s"Line not supported: $info")
    System.exit(0)
  }

  val line = AudioSystem.getLine(info).asInstanceOf[TargetDataLine]
  line.open(format)
  line.start()

  val audio = new AudioInputStream(line)

  val options =
    new RecognizeOptions.Builder().continuous(true).interimResults(true).timestamps(true).wordConfidence(true)
      // .inactivityTimeout(5) // use this to stop listening when the speaker pauses, i.e. for 5s
      .contentType(HttpMediaType.AUDIO_RAW + "; rate=" + sampleRate).build()

  service.recognizeUsingWebSocket(audio, options, new BaseRecognizeCallback() {
    override def onTranscription(speechResults: SpeechResults): Unit = {
      val summary = speechResults.getResults.toList.filter(_.isFinal)
        .map(t => t.getAlternatives.toList.map(_.getTranscript).mkString(" "))
        .mkString(" ")

      if (summary.nonEmpty) listenerBot ! summary
    }
  })

  println("Listening for next 120 seconds")
  Thread.sleep(60 * 1000)

  // closing the WebSockets underlying InputStream will close the WebSocket itself.
  line.stop()
  line.close()

  println("Finished !! ")
}
