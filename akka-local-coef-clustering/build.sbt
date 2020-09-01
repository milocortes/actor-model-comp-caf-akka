name := "akka-local-coef-clustering"

version := "2.0"

val akkaVersion = "2.5.4"

scalaVersion := "2.11.6"

resolvers ++= Seq(
  "Lightbend Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
  "Sonatype Releases" at "http://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.scala-saddle" %% "saddle-core" % "1.3.4"
)
