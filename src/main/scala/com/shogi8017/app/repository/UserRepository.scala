package com.shogi8017.app.repository

import cats.effect.IO
import cats.effect.std.UUIDGen
import com.shogi8017.app.models.{UserModel, UserModelWithPassword}
import doobie.*
import doobie.implicits.*

class UserRepository(trx: Transactor[IO]) {
  def createUser(payload: CreateUserPayload): IO[Either[Throwable, UserModel]] = {
    for {
      uuid <- UUIDGen.randomUUID[IO]
      userId = uuid.toString
      result <- sql"""
        INSERT INTO users (userId, username, password)
        VALUES ($userId, ${payload.username}, ${payload.hashedPassword})
      """.update.run.transact(trx).attempt
    } yield result match {
      case Right(_) => Right(UserModel(userId, payload.username))
      case Left(error) => Left(error)
    }
  }

  def findUserByUsername(payload: FindUserByUsernamePayload): IO[Either[Throwable, Option[UserModelWithPassword]]] = {
    val username = payload.username
    val query: Query0[UserModelWithPassword] = sql"SELECT * FROM users u WHERE u.username = $username".query[UserModelWithPassword]
    query.option.transact(trx).attempt
  }
}

object UserRepository {
  def of(trx: Transactor[IO]): UserRepository = new UserRepository(trx)
}