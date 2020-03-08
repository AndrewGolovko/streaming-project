name := "streaming-ucu-final-project"

ThisBuild / version := "0.1"

scalaVersion := "2.12.8"

// These options will be used for *all* versions.
ThisBuild / scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-encoding", "UTF-8",
  "-Xlint")

val akkaVersion = "2.5.20"
val sparkVersion = "2.4.0"
val jacksonVersion = "2.9.0"

val commonDependencies = Seq(
  "org.apache.kafka" %% "kafka" % "2.1.0" withSources()
    exclude("org.slf4j","slf4j-log4j12")
    exclude("javax.jms", "jms")
    exclude("com.sun.jdmk", "jmxtools")
    exclude("com.sun.jmx", "jmxri"),
  "org.scalatest" %% "scalatest" % "3.0.5" % Test,
  "org.slf4j" % "slf4j-log4j12" % "1.7.25",
  "junit" % "junit" % "4.12" % Test,
  "org.json4s" %% "json4s-native" % "3.6.3"
)

val streamsDependencies = Seq(
  "org.apache.kafka" %% "kafka-streams-scala" % "2.0.1" withSources(),
  "javax.ws.rs" % "javax.ws.rs-api" % "2.1.1" artifacts Artifact("javax.ws.rs-api", "jar", "jar"),
  "org.apache.kafka" % "kafka-streams-test-utils" % "2.0.1" % Test
)

val akkaDependencies = Seq (
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.7" % Test
)

val sparkDependenciesOverrides = Seq(
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion % "provided",
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion % "provided",
  "com.fasterxml.jackson.module" % "jackson-module-scala_2.12" % jacksonVersion % "provided",
)

val sparkDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-sql" % sparkVersion % "provided",
  "org.apache.spark" %% "spark-mllib" % sparkVersion
)

def dockerSettings(debugPort: Option[Int] = None) = Seq(
  dockerfile in docker := {
    val artifactSource: File = assembly.value
    val artifactTargetPath = s"/project/${artifactSource.name}"
    val scriptSourceDir = baseDirectory.value / "../scripts"
    val projectDir = "/project/"
    new Dockerfile {
      from("anapsix/alpine-java:latest")
      add(artifactSource, artifactTargetPath)
      copy(scriptSourceDir, projectDir)
      entryPoint(s"/project/start.sh")
      cmd(projectDir, s"${name.value}", s"${version.value}")
    }
  },
  imageNames in docker := Seq(
    ImageName(
      registry = Some(sys.env("REGISTRY_URI")),
      namespace = Some("ucu-class"),
      repository = name.value,
      tag = Some(s"${sys.env("STUDENT_NAME")}-${version.value}")
    )
//    , ImageName(s"rickerlyman/${name.value}:latest")
  )
)

envFileName in ThisBuild := ".env"

lazy val root = (project in file("."))
  .settings(name := "streaming-ucu-final-project")
  .aggregate(driver_rides_emulator, weather_provider, streaming_app, fare_prediction_app)

lazy val driver_rides_emulator = (project in file("driver-rides-emulator"))
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    name := "driver-rides-emulator",
    libraryDependencies ++= commonDependencies ++ akkaDependencies ++ Seq(
      // your additional dependencies go here
    ),
    dockerSettings()
  )

lazy val weather_provider = (project in file("weather-provider"))
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    name := "weather-provider",
    libraryDependencies ++= commonDependencies ++ akkaDependencies ++ Seq(
      // your additional dependencies go here
    ),
    dockerSettings()
  )

lazy val streaming_app = (project in file("streaming-app"))
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    name := "streaming-app",
    libraryDependencies ++= commonDependencies ++ streamsDependencies ++ Seq(
      // your additional dependencies go here
    ),
    dockerSettings()
  )

lazy val fare_prediction_app = (project in file("fare-prediction-app"))
  .enablePlugins(sbtdocker.DockerPlugin)
  .settings(
    name := "fare-prediction-app",
    mainClass in assembly := Some("ua.ucu.edu.Main"),
    dependencyOverrides ++= sparkDependenciesOverrides,
    libraryDependencies ++= streamsDependencies ++ sparkDependencies ++ Seq(
      // your additional dependencies go here
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "org.apache.kafka" %% "kafka" % "2.1.0" withSources()
        exclude("org.slf4j","slf4j-log4j12")
        exclude("javax.jms", "jms")
        exclude("com.sun.jdmk", "jmxtools")
        exclude("com.sun.jmx", "jmxri"),
      "org.scalatest" %% "scalatest" % "3.0.5" % Test,
      "org.slf4j" % "slf4j-log4j12" % "1.7.25",
      "junit" % "junit" % "4.12" % Test
    ),
    dockerSettings(),
    assemblyMergeStrategy in assembly := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case x => MergeStrategy.first
    }
  )
