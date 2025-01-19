import sbt.Keys.libraryDependencies

lazy val runMigrate = taskKey[Unit]("Migrates the database schema.")
addCommandAlias("run-db-migrations", "runMigrate")

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.2"

val http4sVersion = "1.0.0-M44"
val redis4catsVersion = "1.7.2"
val pureconfigVersion = "0.17.8"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.12.0",
  "org.typelevel" %% "cats-effect" % "3.5.7",

  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.http4s" %% "http4s-server"       % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,

  "com.typesafe" % "config" % "1.4.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.github.pureconfig" %% "pureconfig-core" % pureconfigVersion,

  "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion,
  "dev.profunktor" %% "redis4cats-streams" % redis4catsVersion,

  "ch.qos.logback" % "logback-classic" % "1.5.16",

  "com.github.geirolz" %% "fly4s" % "1.1.0",
  "org.flywaydb" % "flyway-mysql" % "11.2.0",
  "mysql" % "mysql-connector-java" % "8.0.33",
)

lazy val root = (project in file("."))
  .settings(
    name := "chess8007-backend",
    fullRunTask(runMigrate, Compile, "com.chess8007.command.migration.DatabaseMigrationsCommand"),
    runMigrate / fork := true

  )
