package com.shogi8017.app.database

import cats.effect.IO
import cats.effect.kernel.Resource
import doobie.hikari.HikariTransactor

type DatabaseResource = Resource[IO, HikariTransactor[IO]]
