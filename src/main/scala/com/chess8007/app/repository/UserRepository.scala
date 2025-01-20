package com.chess8007.app.repository

import cats.effect.IO
import cats.effect.std.UUIDGen
import com.chess8007.app.database.DatabaseResource
import com.chess8007.app.models.{UserModel, UserModelWithPassword}
import doobie.*
import doobie.implicits.*


class UserRepository(db: DatabaseResource) {
  def createUser(payload: CreateUserPayload): IO[Either[Throwable, UserModel]] = {
    db.use { transactor =>
      for {
        uuid <- UUIDGen.randomUUID[IO]
        userId = uuid.toString
        result <- sql"""
          INSERT INTO users (userId, username, password)
          VALUES ($userId, ${payload.username}, ${payload.hashedPassword})"""
          .update
          .run
          .transact(transactor)
          .attempt
      } yield (userId, result) match {
        case (userId, Right(_)) => Right(UserModel(userId, payload.username))
        case (_, Left(error)) => Left(error)
      }
    }
  }

  def findUserByUsername(payload: FindUserByUsernamePayload): IO[Either[Throwable, Option[UserModelWithPassword]]] = {
    db.use { transactor =>
      val (username) = (payload.username)
      val query: Query0[UserModelWithPassword] = sql"SELECT * FROM users u WHERE (u.username = $username)"
                                    .query[UserModelWithPassword]
      val result: ConnectionIO[Option[UserModelWithPassword]] = query.option
      result.transact(transactor).attempt
    }
  }
}

object UserRepository {
  def of(db: DatabaseResource): UserRepository = new UserRepository(db)
}
