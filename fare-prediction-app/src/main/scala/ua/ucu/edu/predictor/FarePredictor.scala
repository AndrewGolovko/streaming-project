package ua.ucu.edu.predictor

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.LocalFileSystem
import org.apache.hadoop.hdfs.DistributedFileSystem
import org.apache.spark.ml.regression.LinearRegressionModel
import org.apache.spark.ml.{Pipeline, PipelineModel}
import org.apache.spark.ml.tuning.TrainValidationSplitModel
import org.apache.spark.sql.{DataFrame, SparkSession}

import scala.math.max

class FarePredictor {
  val spark: SparkSession = SparkSession
    .builder
    .config("spark.master", "local")
    .getOrCreate()

  val hadoopConfig: Configuration = spark.sparkContext.hadoopConfiguration
  hadoopConfig.set("fs.hdfs.impl", classOf[org.apache.hadoop.hdfs.DistributedFileSystem].getName)
  hadoopConfig.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)

  import spark.implicits._

//  val model: TrainValidationSplitModel = TrainValidationSplitModel.load(
//    System.getProperty("user.dir") + "/fare-prediction-app/src/main/scala/ua/ucu/edu/data/model")
//  "fare-prediction-app/src/main/scala/ua/ucu/edu/data/model")
//  "file://fare-prediction-app/src/main/scala/ua/ucu/edu/data/model")

  val coefficients: List[Double] = List(108.25959144592395,
    -68.41790849100914,63.47345978037886,-41.653077079072844,
    0.0019072719658659212,0.0013675221694017566,-0.006613949833751723)
  val intercept: Double = 17203.080732895916


  def toCelsius: Double => Double = _ - 273.15

  def toDF(list: List[String]): DataFrame = Seq((
    list(0).toDouble,
    list(1).toDouble,
    list(2).toDouble,
    list(3).toDouble,
    list(4).toDouble,
    toCelsius(list(5).toDouble),
    list(7).toDouble
  )).toDF("pickup_longtitude",
    "pickup_lattitude",
    "dropoff_longitude",
    "dropoff_latitude",
    "passenger_count",
    "temp",
    "humidity")

  def clip: Double => Double = x => max(x, 0.0)

  def linearModel(list: List[String]): Double = {
    val seq = Seq(
      list(0).toDouble * coefficients(0) +
      list(1).toDouble * coefficients(1) +
      list(2).toDouble * coefficients(2) +
      list(3).toDouble * coefficients(3) +
      list(4).toDouble * coefficients(4) +
      toCelsius(list(5).toDouble) * coefficients(5) +
      list(7).toDouble * coefficients(6) +
      intercept
    )

    seq.head
  }

  def predict(inputRecord: String): Double = {
//    val df: DataFrame = toDF(inputRecord.split('|').toList)
//    val result: List[Double] = model.transform(df)
//      .select("prediction")
//      .collect()
//      .map(_(0).asInstanceOf[Double])
//      .toList
//    clip(result.head)
    val result: Double = linearModel(inputRecord.split('|').toList)
    clip(result)
  }
}

