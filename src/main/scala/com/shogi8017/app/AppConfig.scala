package com.shogi8017.app

import cats.effect.IO
import pureconfig.{ConfigReader, ConfigSource}

case class AppConfig(app: App, database: DatabaseConfig, jwt: JwtConfig, env: Env) derives ConfigReader

object AppConfig {
  def loadConfig(namespace: String): IO[Either[Throwable, AppConfig]] = IO {
    ConfigSource.default.at(namespace).load[AppConfig] match {
      case Right(config) => Right(config)
      case Left(errors)  => Left(new RuntimeException(s"\n\tConfig loading failed: ${errors.prettyPrint(2)}"))
    }
  }
}

case class DatabaseConfig(
  host: String,
  port: Int,
  databaseName: String,
  user: String,
  password: String,
  migrationsTable: String,
  migrationsLocations: List[String]
) derives ConfigReader {
  def connectionUrl: String = s"jdbc:mysql://$host:$port/$databaseName"
  def prettyPrint: String = {
    s"""
       |Database Configuration:
       |  Host: $host
       |  Port: $port
       |  Database Name: $databaseName
       |  User: ${user}
       |  Password: ${password.map(_ => "****")}
       |  Migrations Table: $migrationsTable
       |  Migrations Locations: ${migrationsLocations.mkString(", ")}
       |""".stripMargin
  }
}

case class JwtConfig(
  algorithm: Option[String],
  accessTokenSecret: String,
  accessTokenTtlSeconds: Int,
  refreshTokenSecret: String,
  refreshTokenTtlSeconds: Int,
  websocketAccessTokenSecret: String,
  websocketAccessTokenTtlSeconds: Int
) derives ConfigReader

case class App(name: String, port: Int, version: String) derives ConfigReader

case class Env(default: String) derives ConfigReader