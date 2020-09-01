name := "akka-distribuido-coef-clustering"

version := "1.0"

val akkaVersion = "2.6.4"

resolvers ++= Seq(
  "Lightbend Repository" at "http://repo.typesafe.com/typesafe/releases/"
)

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-remote" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-serialization-jackson" % "2.6.4",
  // Last stable release
  "org.scalanlp" %% "breeze" % "1.1",
// Native libraries are not included by default. add this if you want them
// Native libraries greatly improve performance, but increase jar sizes.
// It also packages various blas implementations, which have licenses that may or may not
// be compatible with the Apache License. No GPL code, as best I know.
  "org.scalanlp" %% "breeze-natives" % "1.1",
// The visualization library is distributed separately as well.
// It depends on LGPL code
  "org.scalanlp" %% "breeze-viz" % "1.1"
)
