package com.chess8007.app.services

import cats.effect.IO
import com.chess8007.app.database.DatabaseResource
import com.chess8007.app.errors.IncorrectUsernameOrPassword
import com.chess8007.app.models.UserModel
import com.chess8007.app.repository.{CreateUserPayload, FindUserByUsernamePayload, UserRepository}
import com.chess8007.app.routes.{UserLoginPayload, UserSignUpPayload}
import org.mindrot.jbcrypt.BCrypt

class UserService(db: DatabaseResource) {
  private val userRepo = UserRepository.of(db)

  def authenticateUser(payload: UserLoginPayload): IO[Either[Throwable, Option[UserModel]]] = {
    val findUserByCredentialsPayload = FindUserByUsernamePayload(payload.username)
    val userOptionIO = userRepo.findUserByUsername(findUserByCredentialsPayload)

    userOptionIO.map(userEither => userEither.flatMap {
      case Some(user) =>
        if (BCrypt.checkpw(payload.password, user.password))
          Right(Some(UserModel.fromUserModelWithPassword(user)))
        else
          Left(IncorrectUsernameOrPassword)
      case None => Right(None)
    })
  }

  def signUpUser(payload: UserSignUpPayload): IO[Either[Throwable, UserModel]] = {
    val hashedPassword = BCrypt.hashpw(payload.password, BCrypt.gensalt());
    val createUserPayload = CreateUserPayload(payload.username, hashedPassword)
    userRepo.createUser(createUserPayload)
  }
}

object UserService {
  def of(db: DatabaseResource): UserService = new UserService(db)
}