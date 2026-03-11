scalaVersion := "2.13.18"

val sttpVersion = "3.11.0"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core",
  "com.softwaremill.sttp.client3" %% "circe",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats",
).map(_ % sttpVersion)

val circeVersion = "0.14.15"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
).map(_ % circeVersion)

// https://typelevel.org/cats-effect/docs/tutorial

libraryDependencies += "org.typelevel" %% "cats-effect" % "3.7.0"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds",
  // "-Ypartial-unification"
)
