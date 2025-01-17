import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.4"

val http4sVersion = "1.0.0-M44"
val redis4catsVersion = "1.7.2"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.12.0",
  "org.typelevel" %% "cats-effect" % "3.5.7",

  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.http4s" %% "http4s-server"       % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,

  "com.typesafe" % "config" % "1.4.3",

  "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion,
  "dev.profunktor" %% "redis4cats-streams" % redis4catsVersion,
)

lazy val root = (project in file("."))
  .settings(
    name := "chess8007-backend"
  )
