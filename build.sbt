name := "reactive-stocks-s4n"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.2"

val akka = "2.3.6"

libraryDependencies ++= Seq(
    ws, // Play's web services module
    "com.typesafe.akka" %% "akka-actor" % akka,
    "com.typesafe.akka" %% "akka-slf4j" % akka,
    "org.webjars" % "bootstrap" % "2.3.1",
    "org.webjars" % "flot" % "0.8.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.2"
)