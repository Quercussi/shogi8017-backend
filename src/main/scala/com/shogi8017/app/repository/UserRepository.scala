package com.shogi8017.app.repository

import cats.data.NonEmptyList
import cats.effect.IO
import cats.effect.std.UUIDGen
import cats.implicits.catsSyntaxList
import cats.implicits.toFoldableOps
import cats.syntax.apply.catsSyntaxTuple2Semigroupal
import com.shogi8017.app.models.{UserModel, UserModelWithPassword}
import doobie.*
import doobie.implicits.*
import doobie.util.fragments._

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

  def getUserById(payload: GetUserPayload): IO[Either[Throwable, Option[UserModel]]] = {
    val userId = payload.userId
    val query: Query0[UserModel] = sql"SELECT * FROM users u WHERE u.userId = $userId".query[UserModel]
    query.option.transact(trx).attempt
  }
  
  def findUserByUsername(payload: FindUserByUsernamePayload): IO[Either[Throwable, Option[UserModelWithPassword]]] = {
    val username = payload.username
    val query: Query0[UserModelWithPassword] = sql"SELECT * FROM users u WHERE u.username = $username".query[UserModelWithPassword]
    query.option.transact(trx).attempt
  }

  def paginatedSearchUser(payload: PaginatedSearchUserPayloadRepo): IO[Either[Throwable, PaginatedSearchUserResponseRepo]] = {
    val searchQuery = s"%${payload.searchQuery.toLowerCase}%"
    val limit = payload.limit
    val offset = payload.offset
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
        """ ++
          exclusionFilter ++
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
        """ ++
          exclusionFilter

      baseQuery.query[Int]
    }

    (usersQuery.to[List], countQuery.unique).mapN { (users, total) =>
      val nextOffset = if (offset + limit < total) offset + limit else -1
      PaginatedSearchUserResponseRepo(users, nextOffset, total)
    }.transact(trx).attempt
  }
}

object UserRepository {
  def of(trx: Transactor[IO]): UserRepository = new UserRepository(trx)
}