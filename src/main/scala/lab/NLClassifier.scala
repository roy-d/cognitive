package lab

import akka.actor.{Actor, ActorLogging, Props}
import com.ibm.watson.developer_cloud.natural_language_classifier.v1.NaturalLanguageClassifier
import lab.NLClassifier.{NLClassifierConfig, NLClassifierResponse}
import lab.TwitterBot.Payload

import scala.collection.JavaConversions._

class NLClassifier(nlClassifierConf: NLClassifierConfig) extends Actor with ActorLogging {
  val service = new NaturalLanguageClassifier()
  service.setUsernameAndPassword(nlClassifierConf.userName, nlClassifierConf.password)

  /**
    * Training NLC:
    * log.info(service.createClassifier("conditions", "en", new File("/Users/droy/Dev/cognitive/src/main/resources/training/condition_nlc_train.csv")).execute().getId)
    * List("ICD-10-GT-AA", "ICD-10-GT-AAA", "ICD-10-GT-BB", "ICD-10-GT-CC", "ICD-10-GT-DD", "ICD-10-GT-EE")
    * .foreach(
    * file => log.info(service.createClassifier(file, "en", new File(s"/Users/droy/Dev/cognitive/src/main/resources/training/icd10/$file.csv")).execute().getId))
    */

  def receive = {
    case payload: Payload =>
      val classifiers = service.getClassifiers.execute().getClassifiers.toList.map(classifier => (classifier.getId, classifier.getName))

      log.info(s"Classifiers: $classifiers")

      val classifications =
        classifiers.map { case (classifierId, classifierName) => classifierName -> service.classify(classifierId, payload.tweet).execute() }
          .toMap
          .mapValues(classification => classification.getClasses.toList.sortWith(_.getConfidence > _.getConfidence).head)
          .groupBy { case (k, v) => k.take(9) }
          .mapValues(v => v.values.toList.sortWith(_.getConfidence > _.getConfidence).head)
          .mapValues(classification => f"${classification.getName}%s(${classification.getConfidence}%1.1f)")
          .toString.replace("Map", "").replace(" -> ", ":")

      sender() ! NLClassifierResponse(classifications, payload)
  }
}

object NLClassifier {

  case class NLClassifierConfig(userName: String, password: String)

  case class NLClassifierResponse(response: String, payload: Payload)

  def props(nlClassifierConf: NLClassifierConfig): Props = Props(new NLClassifier(nlClassifierConf))
}
