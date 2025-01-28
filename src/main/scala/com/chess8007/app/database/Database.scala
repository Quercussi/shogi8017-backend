package com.chess8007.app.database

import cats.effect.IO
import cats.effect.kernel.Resource
import com.chess8007.app.AppConfig
import doobie.hikari.HikariTransactor
import doobie.util.ExecutionContexts

class Database(appConfig: AppConfig) {
  lazy val db: DatabaseResource = for {
    ce <- ExecutionContexts.fixedThreadPool[IO](32)
    xa <- HikariTransactor.newHikariTransactor[IO](
      "com.mysql.cj.jdbc.Driver",
      appConfig.database.connectionUrl,
      appConfig.database.user,
      appConfig.database.password,
      ce
    )
  } yield xa
  
  def database: DatabaseResource = db
}

object Database {
  def of(appConfig: AppConfig): Database = new Database(appConfig)

  def instantiateDb(appConfig: AppConfig): DatabaseResource =
    of(appConfig).database
}