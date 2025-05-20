package com.shogi8017.app.database

import cats.effect.IO
import com.shogi8017.app.AppConfig
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.connection.RedisClient
import dev.profunktor.redis4cats.data.RedisCodec
import dev.profunktor.redis4cats.effect.Log.Stdout._

class RedisDatabase(appConfig: AppConfig) {
  def redis: RedisResource = {
    for {
      client <- RedisClient[IO].from(appConfig.redis.connectionUrl)
      commands <- Redis[IO].fromClient(client, RedisCodec.Utf8)
    } yield commands
  }
}

object RedisDatabase {
  def of(appConfig: AppConfig): RedisDatabase = new RedisDatabase(appConfig)

  def commands(appConfig: AppConfig): RedisResource =
    of(appConfig).redis
}