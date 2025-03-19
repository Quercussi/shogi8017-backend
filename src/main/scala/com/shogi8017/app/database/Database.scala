package com.shogi8017.app.database

import cats.effect.{IO, Resource}
import com.shogi8017.app.AppConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

class Database(appConfig: AppConfig) {
  def database: DatabaseResource = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "com.mysql.cj.jdbc.Driver",
      appConfig.database.connectionUrl,
      appConfig.database.user,
      appConfig.database.password,
      ce,
    )
  } yield xa
}

object Database {
  def of(appConfig: AppConfig): Database = new Database(appConfig)

  def transactor(appConfig: AppConfig): DatabaseResource =
    of(appConfig).database
}