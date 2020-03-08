package ua.ucu.edu.actor

import akka.actor.{Actor, ActorLogging, Props}
import ua.ucu.edu.feature.{Feature, FeatureApi, FeatureGenerator}

import scala.language.postfixOps

object FeatureActor {
  def props(featureId: String, featureType: Feature): Props =
    Props(new FeatureActor(featureId, featureType))
}

class FeatureActor(val featureId: String, featureType: Feature) extends Actor with ActorLogging{

  val api: FeatureApi = new FeatureGenerator

  override def preStart(): Unit = {
    log.info("[Started] Feature {}:{}", featureId, featureType.toString)
  }

  override def postStop(): Unit = {
    log.info("[Stopped] Feature {}:{}", featureId, featureType.toString)
  }

  override def receive: Receive = {
    case ReadFeature =>
      sender ! RespondFeature(
        featureId,
        featureType.toString,
        api.readCurrentValue(featureType)
      )
  }

}
