package actor

import java.util.Properties

import akka.actor.{Actor, ActorLogging, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import ua.ucu.edu.Main.system
import ua.ucu.edu.model.Location
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}
import scala.concurrent.Future

object Config {
  val KafkaBrokers = "KAFKA_BROKERS"
}

class WheatherActor extends Actor with ActorLogging {
  import WheatherActor._
  import system.dispatcher

  val logger = LoggerFactory.getLogger(getClass)

  implicit val actorSystem: ActorSystem = context.system

  val BrokerList: String = System.getenv(Config.KafkaBrokers)
  // Initialization of topic to push info about weather
  val topic = "weather-new-york"
  val props = new Properties()

  logger.info("[Kafka] Started topic: {}", topic)
  props.put("bootstrap.servers", BrokerList)
//  props.put("bootstrap.servers", "localhost:9092")
  props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
  props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

  val producer = new KafkaProducer[String, String](props)
  val apiURL = "http://api.openweathermap.org/data/2.5/weather/"
  val apiKEY = "e6fb856a1e439a9d0fbf774e18bc87a2"

  var i = 0

  override def receive: Receive = {
    case WeatherRequest => {
      val loc = Locations.getItem()
      getWeather(loc.latitude.toString, loc.longitude.toString)
        .onComplete {
          case Success(s) => {

            val str = s._3.toString.split("json,")(1).dropRight(1).replaceAll("\"", "")

            val v1 = findValueForTemp(str)
            val v2 = findValueForPressure(str)
            val v3 = findValueForHumidity(str)

            val output = '|' + v1 + '|'+ v2 + '|' + v3

            val data = new ProducerRecord[String, String](topic, "NY",  output)

            producer.send(data)
          }
          case Failure(f) => {
            println(f.getMessage)
          }
        }
    }
  }

  def findValueForTemp(str: String): String = {
    val regex = """(temp?):\s*\.*(\d*.\d*)""".r
    val matcher = regex.pattern.matcher(str)
    matcher.find()
    matcher.group(2)
  }

  def findValueForHumidity(str: String): String = {
    val regex = """(humidity?):\s*\.*(\d*)""".r
    val matcher = regex.pattern.matcher(str)
    matcher.find()
    matcher.group(2)
  }

  def findValueForPressure(str: String): String = {
    val regex = """(pressure?):\s*\.*(\d*)""".r
    val matcher = regex.pattern.matcher(str)
    matcher.find()
    matcher.group(2)
  }

  def getWeather(lon: String, lat: String): Future[HttpResponse] = {
    val url = Uri(apiURL)
    Http().singleRequest(
      HttpRequest(
        HttpMethods.GET,
        url.withQuery(Uri.Query(
          "lon" -> lon,
          "lat" -> lat,
          "APPID" -> apiKEY,
        ))
      )
    )
  }

  override def postStop(): Unit = {
    producer.close()
  }
}

object WheatherActor {
  case object WeatherRequest
  case object Locations {
    private var locations: List[Location] = List[Location]()
    var i = 0
    def set(loc: List[Location]): Unit = {
      locations = loc
    }
    def get(): List[Location] = locations
    def getItem(): Location = {locations(i)}
  }
}