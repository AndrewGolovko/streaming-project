package ua.ucu.edu

import java.util.Properties
import java.util.concurrent.TimeUnit

import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.slf4j.LoggerFactory
import ua.ucu.edu.predictor.FarePredictor

object Main extends App {
  val logger = LoggerFactory.getLogger(getClass)

  println("logger")

  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "fare-prediction-app")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv(Config.KafkaBrokers))
//  props.put("bootstrap.servers", "localhost:9092")
  props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, Long.box(5 * 1000))
  props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, Long.box(0))

  import Serdes._

  val builder: StreamsBuilder = new StreamsBuilder
  val predictor: FarePredictor = new FarePredictor

  // gets (key, drivers_weather_data string) and returns (key, predicted_fare)
  val data_stream = builder.stream[String, String]("drivers_weather_data").map(
    (key, value) => (key, predictor.predict(value).toString))

  data_stream.peek((key, value) => println(key, value))

  data_stream.foreach { (k, v) =>
    logger.info(s"main record $k->$v")
  }

  data_stream.to("fare-prediction")

  val streams = new KafkaStreams(builder.build(), props)
  streams.cleanUp()
  streams.start()

  sys.addShutdownHook {
    streams.close(10, TimeUnit.SECONDS)
  }

  object Config {
    val KafkaBrokers = "KAFKA_BROKERS"
  }
}
