package com.shogi8017.app.database

import cats.effect.{IO, Resource}
import dev.profunktor.redis4cats.RedisCommands

type RedisResource = Resource[IO, RedisCommands[IO, String, String]]
