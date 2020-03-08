package ua.ucu.edu

import akka.actor._
import ua.ucu.edu.actor.DriverActor
import ua.ucu.edu.actor.DriverActor.RegisterRide

import scala.collection.mutable.ListBuffer
import scala.util.Random

object Main extends App {

  implicit val system: ActorSystem = ActorSystem()

  val names = List(
    "Maksik", "Andriusha", "Mykolka"
  )

  val driversBuff = ListBuffer[ActorRef]()

  // creating 3 different drivers
  for (i <- 0 to 2) {
    driversBuff += system.actorOf(Props(classOf[DriverActor], names(i)), "driver-" + names(i).toLowerCase)
  }

  val drivers = driversBuff.toList
//  Creating Rides generators for riders
  for (i <- 0 to 2)
    for (j <- 1 to 5)
      drivers(i) ! RegisterRide(names(i).toLowerCase + "-" + j.toString + "-" + Random.nextInt(100))

}