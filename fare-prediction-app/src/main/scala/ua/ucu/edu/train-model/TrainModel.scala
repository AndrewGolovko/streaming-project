package ua.ucu.edu.model

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.evaluation.RegressionEvaluator
import org.apache.spark.ml.feature.VectorAssembler
import org.apache.spark.ml.regression.{LinearRegression, LinearRegressionModel}
import org.apache.spark.ml.tuning.{ParamGridBuilder, TrainValidationSplit}
import org.apache.spark.sql.SparkSession


object TrainModel extends App {
  println("start")
  val spark = SparkSession
    .builder()
    .config("spark.master", "local")
    .getOrCreate()

  println("read csv")
  val training = spark.read.format("csv")
    .option("header", "true")
    .option("inferSchema", "true")
    .load(
      System.getProperty("user.dir") +
      "/fare-prediction-app/src/main/scala/ua/ucu/edu/data/train_dataset_small.csv")
    .cache()

  training.printSchema()

  val assembler = new VectorAssembler()
    .setInputCols(Array(
      "pickup_longtitude",
      "pickup_lattitude",
      "dropoff_longitude",
      "dropoff_latitude",
      "passenger_count",
      "temp",
      "humidity"))
    .setOutputCol("features")

  println("create model")
  val lr = new LinearRegression()
    .setLabelCol("fare_amount")
    .setFeaturesCol("features")
    .setMaxIter(10)
    .setRegParam(0.3)
    .setElasticNetParam(0.8)

  val paramGrid = new ParamGridBuilder()
    .addGrid(lr.regParam, Array(0.1, 0.01))
    .addGrid(lr.fitIntercept)
    .addGrid(lr.elasticNetParam, Array(0.0, 1.0))
    .build()


  val steps: Array[org.apache.spark.ml.PipelineStage] = Array(assembler, lr)
  val pipeline = new Pipeline().setStages(steps)

  val tvs = new TrainValidationSplit()
    .setEstimator(pipeline) // the estimator can also just be an individual model rather than a pipeline
    .setEvaluator(new RegressionEvaluator().setLabelCol("fare_amount"))
    .setEstimatorParamMaps(paramGrid)
    .setTrainRatio(0.75)

  val model = tvs.fit(training)

  lr.weightCol
  pipeline.getStages.last
  println(model.getEstimator.asInstanceOf[Pipeline].getStages.last.asInstanceOf[LinearRegressionModel].coefficients)
  println(model.getEstimator.asInstanceOf[Pipeline].getStages.last.asInstanceOf[LinearRegressionModel].intercept)
  println(model.bestModel.asInstanceOf[LinearRegressionModel].coefficients)
  println(model.bestModel.asInstanceOf[LinearRegressionModel].intercept)

//    model.save("/fare-prediction-app/src/main/scala/data/model")
}
