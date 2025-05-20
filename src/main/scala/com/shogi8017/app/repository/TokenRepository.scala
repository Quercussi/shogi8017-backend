package com.shogi8017.app.repository

import cats.data.EitherT
import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.database.RedisResource
import org.typelevel.log4cats.slf4j.Slf4jLogger

import scala.concurrent.duration.*

class TokenRepository(redisResource: RedisResource) {
  private val websocketTokenPrefix = "ws_token:"

  def storeWebSocketToken(token: String, ttlSeconds: Int): EitherT[IO, Throwable, Unit] = {
    EitherT {
      redisResource.use { redis =>
        redis.setEx(
          s"$websocketTokenPrefix$token",
          "",
          ttlSeconds.seconds
        ).attempt
      }
    }
  }

  def validateAndConsumeWebSocketToken(token: String): IO[Boolean] = {
    redisResource.use { redis =>
      for {
        exists <- redis.get(s"$websocketTokenPrefix$token")
        result <- exists match {
          case Some(_) => redis.del(s"$websocketTokenPrefix$token").map(_ > 0)
          case None => IO.pure(false)
        }
      } yield result
    }
  }

  private def createRedisKey(token: String): String = {
    s"$websocketTokenPrefix$token"
  }
}

object TokenRepository {
  def of(redisResource: RedisResource): TokenRepository =
    new TokenRepository(redisResource)
}