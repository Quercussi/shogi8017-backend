package com.chess8007.app.services

import cats.effect.{IO, MonadCancelThrow, Resource}
import com.chess8007.app.{AppConfig, JwtConfig}
import com.chess8007.app.database.DatabaseResource
import com.chess8007.app.models.UserModel
import com.chess8007.app.repository.{CreateUserPayload, FindUserByCredentialsPayload, UserRepository}
import com.chess8007.app.routes.{UserLoginPayload, UserSignUpPayload}
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor

class UserService(db: DatabaseResource) {
  private val userRepo = UserRepository.of(db)
  
  def authenticateUser(payload: UserLoginPayload): IO[Either[Throwable, Option[UserModel]]] = {
    val hashedPassword = payload.password // TODO: hash the thing
    val findUserByCredentialsPayload = FindUserByCredentialsPayload(payload.username, hashedPassword)
    userRepo.findUserByCredentials(findUserByCredentialsPayload)
  }

  def signUpUser(payload: UserSignUpPayload): IO[Either[Throwable, UserModel]] = {
    val hashedPassword = payload.password // TODO: hash the thing
    val createUserPayload = CreateUserPayload(payload.username, hashedPassword)
    userRepo.createUser(createUserPayload)
  }
}

object UserService {
  def of(db: DatabaseResource): UserService = new UserService(db)
}