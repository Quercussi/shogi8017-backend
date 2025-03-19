package com.shogi8017.app.repository

import cats.data.{EitherT, NonEmptyList}
import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.implicits._
import com.shogi8017.app.models.{UserModel, UserModelWithPassword}
import doobie._
import doobie.implicits._
import doobie.util.fragments._

class UserRepository(trx: Transactor[IO]) {

  def createUser(payload: CreateUserPayload): EitherT[IO, Throwable, UserModel] = {
    EitherT {
      for {
        uuid   <- UUIDGen.randomUUID[IO]
        userId  = uuid.toString
        result <- sql"""
          INSERT INTO users (userId, username, password)
          VALUES ($userId, ${payload.username}, ${payload.hashedPassword})
        """.update.run.transact(trx).attempt
      } yield result match {
        case Right(_)    => Right(UserModel(userId, payload.username))
        case Left(error) => Left(error)
      }
    }
  }

  def getUserById(payload: GetUserByIdPayloadRepo): EitherT[IO, Throwable, Option[UserModel]] =
    EitherT {
      sql"SELECT * FROM users u WHERE u.userId = ${payload.userId}"
        .query[UserModel]
        .option
        .transact(trx)
        .attempt
    }

  def findUserByUsername(payload: FindUserByUsernamePayload): EitherT[IO, Throwable, Option[UserModelWithPassword]] =
    EitherT {
      sql"SELECT * FROM users u WHERE u.username = ${payload.username}"
        .query[UserModelWithPassword]
        .option
        .transact(trx)
        .attempt
    }

  def paginatedSearchUser(payload: PaginatedSearchUserPayloadRepo): EitherT[IO, Throwable, PaginatedSearchUserResponseRepo] = {
    val searchQuery     = s"%${payload.searchQuery.toLowerCase}%"
    val limit           = payload.limit
    val offset          = payload.offset
    val excludingUserIds = payload.excludingUserIds

    val exclusionFilter: Fragment = excludingUserIds match {
      case Nil => Fragment.empty
      case nonEmptyList =>
        NonEmptyList.fromList(nonEmptyList) match {
          case Some(ids) =>
            val fragments = ids.map(id => fr"$id")
            fr"AND userId NOT IN (" ++ fragments.intercalate(fr",") ++ fr")"
          case None => Fragment.empty
        }
    }

    val usersQuery: Query0[UserModel] = {
      val baseQuery =
        fr"""
          SELECT userId, username
          FROM users
          WHERE LOWER(username) LIKE $searchQuery
        """ ++ exclusionFilter ++
          fr"""
          ORDER BY username ASC
          LIMIT $limit OFFSET $offset
        """
      baseQuery.query[UserModel]
    }

    val countQuery: Query0[Int] = {
      val baseQuery =
        fr"""
          SELECT COUNT(*) FROM users
          WHERE LOWER(username) LIKE $searchQuery
        """ ++ exclusionFilter
      baseQuery.query[Int]
    }

    EitherT {
      (usersQuery.to[List], countQuery.unique).mapN { (users, total) =>
        val nextOffset = if (offset + limit < total) offset + limit else -1
        PaginatedSearchUserResponseRepo(users, nextOffset, total)
      }.transact(trx).attempt
    }
  }
}

object UserRepository {
  def of(trx: Transactor[IO]): UserRepository = new UserRepository(trx)
}