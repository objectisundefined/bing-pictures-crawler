scalaVersion := "2.13.4"

val sttpVersion = "3.3.18"

libraryDependencies ++= Seq(
  "com.softwaremill.sttp.client3" %% "core",
  "com.softwaremill.sttp.client3" %% "circe",
  "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats",
).map(_ % sttpVersion)

val circeVersion = "0.14.1"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser",
).map(_ % circeVersion)

// https://typelevel.org/cats-effect/docs/2.x/guides/tutorial

libraryDependencies += "org.typelevel" %% "cats-effect" % "2.5.3"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-unchecked",
  "-language:postfixOps",
  "-language:higherKinds",
  // "-Ypartial-unification"
)
