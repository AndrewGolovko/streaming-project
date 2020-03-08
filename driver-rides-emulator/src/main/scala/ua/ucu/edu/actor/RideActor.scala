package ua.ucu.edu.actor

import java.util.Properties

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import ua.ucu.edu.feature.TripFeatures

import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps

object RideActor {

  def props(rideId: String): Props = Props(new RideActor(rideId))
  case object RegisterFeatures
  case object FeaturesRegistered
}


object Config {
  val KafkaBrokers = "KAFKA_BROKERS"
}

class RideActor(val rideId: String) extends Actor with ActorLogging {

  import RideActor._

  val BrokerList: String = System.getenv(Config.KafkaBrokers)
  val topic = "rides-info"
  val props = new Properties()

  log.info("[Stream] Started for ride {}", rideId)

  props.put("bootstrap.servers", BrokerList)
//  props.put("bootstrap.servers", "localhost:9092")
  props.put("client.id", "Trip:" + rideId)
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](props)



  val featureToActor = mutable.Map.empty[String, ActorRef]
  val actorToFeature = mutable.Map.empty[ActorRef, String]

  override def preStart(): Unit = {
    log.info("[Started] Ride : {}", rideId)
    context.system.scheduler.schedule(5 second, 10 seconds, self, ReadFeature)(context.dispatcher, self)
  }

  override def postStop(): Unit = {
    log.info("[Finished] Ride : {}", rideId)
    producer.close()
  }


  override def receive: Receive = {

    case ReadFeature => {
      for (child <- actorToFeature.keySet) {
        child forward ReadFeature
      }
    }

    case RespondFeature(featureId, featureType, value) => {
      log.info("[Push] Sending message about ride {} to topic {}", rideId, topic)

      val message =
        featureId +  "|" + value

      val data = new ProducerRecord[String, String](topic, message)
      producer.send(data)

      log.info("[Success] Sent message about ride {} to topic {}", rideId, topic)
    }

    case RegisterFeatures => {

      val tripFeatures = context.actorOf(Props(classOf[FeatureActor], rideId, TripFeatures), rideId + ":" + TripFeatures.toString)
      context.watch(tripFeatures)

      featureToActor += rideId + ":" + TripFeatures.toString -> tripFeatures

      actorToFeature += tripFeatures -> (rideId + ":" + TripFeatures.toString)

    }

    case Terminated(feature) => {
      val featureId = actorToFeature(feature)
      log.info("[Terminated] Feature: {}", rideId)
      featureToActor -= featureId
      actorToFeature -= feature
    }
  }
}
