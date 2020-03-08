package ua.ucu.edu

import java.util.Properties
import java.util.concurrent.TimeUnit

import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.slf4j.LoggerFactory

object Main extends App {

  val logger = LoggerFactory.getLogger(getClass)

  println("logger")

  val props = new Properties()
  props.put(StreamsConfig.APPLICATION_ID_CONFIG, "streaming_app")
  props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv(Config.KafkaBrokers))
//  props.put("bootstrap.servers", "localhost:9092")
  props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, Long.box(5 * 1000))
  props.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, Long.box(0))

  import Serdes._

  val builder:StreamsBuilder = new StreamsBuilder

  val drivers_data_stream = builder.stream[String, String]("rides-info").map(
    (_, value) => ("NY", value.split('|').slice(1, 999).mkString("|")))

  val weather_data_table = builder.table[String, String]("weather-new-york")

  val main_stream = drivers_data_stream.join(weather_data_table)(
    (tripFeaturesValue, weatherValue) => tripFeaturesValue +  weatherValue)

  main_stream.peek((key, value) => println(key, value))

  main_stream.foreach { (k, v) =>
    logger.info(s"main record $k->$v")
  }

  main_stream.to("drivers_weather_data")

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
