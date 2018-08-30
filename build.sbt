name := "Cricket"

organization := "com.cricket360"

version := "0.1"

scalaVersion := "2.12.6"

libraryDependencies ++= Seq(
  apachePOI,
  mongo_casbah,
  lift_json
)

val apachePOI = "org.apache.poi" % "poi-ooxml" % "3.9"

val mongo_casbah = "org.mongodb" %% "casbah" % "3.1.1"

val lift_json = "net.liftweb" %% "lift-json" % "3.0.1"


scalacOptions += "-deprecation"