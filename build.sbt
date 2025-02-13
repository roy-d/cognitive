name := "cognitive"

version := "1.0"

scalaVersion := "2.11.8"

fork in run := true

connectInput in run := true

libraryDependencies ++= Seq(
  "org.twitter4j" % "twitter4j-stream" % "4.0.5",
  "com.typesafe" % "config" % "1.3.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.10",
  "com.typesafe.akka" %% "akka-testkit" % "2.4.10",
  "com.ibm.watson.developer_cloud" % "java-sdk" % "3.3.1",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)
