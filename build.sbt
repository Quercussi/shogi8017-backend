import sbt.Keys.libraryDependencies

lazy val runMigrate = taskKey[Unit]("Migrates the database schema.")
addCommandAlias("run-db-migrations", "runMigrate")

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.5.2"

val http4sVersion = "0.23.30"
val jwtHttp4sVersion = "2.0.2"
val jwtScalaVersion = "10.0.1"
val doobieVersion = "1.0.0-RC6"
val redis4catsVersion = "1.7.2"
val pureconfigVersion = "0.17.8"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "2.13.0",
  "org.typelevel" %% "cats-effect" % "3.5.7",

  "org.scalatest" %% "scalatest" % "3.2.19" % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.6.0" % Test,

  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.http4s" %% "http4s-server"       % http4sVersion,
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-circe"        % http4sVersion,

  "dev.profunktor" %% "http4s-jwt-auth"    % jwtHttp4sVersion,
  "com.github.jwt-scala"    %% "jwt-core"            % jwtScalaVersion,
  "com.github.jwt-scala"    %% "jwt-circe"           % jwtScalaVersion,

  "com.typesafe" % "config" % "1.4.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "com.github.pureconfig" %% "pureconfig-core" % pureconfigVersion,

  "org.tpolecat" %% "doobie-core"     % doobieVersion,
  "org.tpolecat" %% "doobie-mysql"    % doobieVersion,
  "org.tpolecat" %% "doobie-hikari"   % doobieVersion,

  "org.mindrot" % "jbcrypt" % "0.4",

  "dev.profunktor" %% "redis4cats-effects" % redis4catsVersion,
  "dev.profunktor" %% "redis4cats-streams" % redis4catsVersion,

  "dev.profunktor" %% "fs2-rabbit-circe" % "5.3.0",

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
