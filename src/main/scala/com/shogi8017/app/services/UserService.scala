package com.shogi8017.app.services

import cats.effect.IO
import com.shogi8017.app.database.DatabaseResource
import com.shogi8017.app.exceptions.IncorrectUsernameOrPassword
import com.shogi8017.app.models.UserModel
import com.shogi8017.app.repository.{CreateUserPayload, FindUserByUsernamePayload, PaginatedSearchUserPayloadRepo, PaginatedSearchUserResponseRepo, UserRepository}
import com.shogi8017.app.routes.{PaginatedSearchUserPayload, PaginatedSearchUserResponse, UserLoginPayload, UserSignUpPayload}
import org.mindrot.jbcrypt.BCrypt

class UserService(userRepository: UserRepository) {
  def authenticateUser(payload: UserLoginPayload): IO[Either[Throwable, Option[UserModel]]] = {
    val findUserByCredentialsPayload = FindUserByUsernamePayload(payload.username)
    val userOptionIO = userRepository.findUserByUsername(findUserByCredentialsPayload)

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
    userRepository.createUser(createUserPayload)
  }
  
  def paginatedSearchUser(payload: PaginatedSearchUserPayload): IO[Either[Throwable, PaginatedSearchUserResponse]] = {
    val res = userRepository.paginatedSearchUser(PaginatedSearchUserPayloadRepo.fromPaginatedSearchUserPayload(payload))
    res.map(_.map(PaginatedSearchUserResponseRepo.toPaginatedSearchUserResponse))
  }
}

object UserService {
  def of(userRepository: UserRepository): UserService = new UserService(userRepository)
}