package ua.ucu.edu.actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import ua.ucu.edu.actor.RideActor.RegisterFeatures

import scala.collection.mutable

object DriverActor {
  final case class RegisterRide(rideId: String)
  case object RideRegistered
}

class DriverActor(driverName: String) extends Actor with ActorLogging {

  import DriverActor._

  lazy val rideToActor = mutable.Map.empty[String, ActorRef]
  lazy val actorToRide = mutable.Map.empty[ActorRef, String]

  override def preStart(): Unit = log.info("[Created] Driver {}", driverName)

  override def postStop(): Unit = log.info("[Dropped] Driver {}", driverName)


  override def receive: Receive = {
    case registerMassage @ RegisterRide(rideId) => {
      rideToActor.get(rideId) match {
        case Some(ref) =>
          log.info("[Warning] Ride : {} already exists", rideId)
        case None =>
          log.info("[Started] Ride : {}", rideId)
          val ride = context.actorOf(Props(classOf[RideActor], rideId), "ride-" + rideId)
          context.watch(ride)
          ride forward RegisterFeatures
          rideToActor += rideId -> ride
          actorToRide += ride -> rideId
      }
    }

    case Terminated(ride) => {
      val rideId = actorToRide(ride)
      log.info("[Stopped] Ride : {}", rideId)
      actorToRide -= ride
      rideToActor -= rideId
    }
  }
}
