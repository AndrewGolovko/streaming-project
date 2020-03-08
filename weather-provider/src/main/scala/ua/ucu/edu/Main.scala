package ua.ucu.edu

import actor.WheatherActor
import akka.actor.{ActorSystem, Props}
import org.slf4j.LoggerFactory

import scala.concurrent.duration
import scala.language.postfixOps

import ua.ucu.edu.model.Location

object Main extends App {

  // initializing New York location
  val NY: List[Location] = List(Location((-74.01).toFloat, (40.71).toFloat))
  WheatherActor.Locations.set(NY)

  val logger = LoggerFactory.getLogger(getClass)
  logger.info("Weather App Started ... ")

  val system = ActorSystem()
  import system.dispatcher
  import duration._

  val actor = system.actorOf(Props[WheatherActor], "WhetherActor")

  // scheduling updates
  system.scheduler.schedule(1 seconds, 10 seconds, actor, WheatherActor.WeatherRequest)
}
