package com.shogi8017.app.services

import cats.data.EitherT
import cats.effect.IO
import com.shogi8017.app.exceptions.IncorrectUsernameOrPassword
import com.shogi8017.app.models.{UserModel, UserModelWithPassword}
import com.shogi8017.app.repository.{CreateUserPayload, FindUserByUsernamePayload, PaginatedSearchUserPayloadRepo, PaginatedSearchUserResponseRepo, UserRepository}
import com.shogi8017.app.routes.{PaginatedSearchUserPayload, PaginatedSearchUserResponse, UserLoginPayload, UserSignUpPayload}
import org.mindrot.jbcrypt.BCrypt

class UserService(userRepository: UserRepository) {

  def authenticateUser(payload: UserLoginPayload): EitherT[IO,Throwable, Option[UserModel]] = {
    val findUserPayload = FindUserByUsernamePayload(payload.username)
    userRepository.findUserByUsername(findUserPayload)
      .flatMap(userOpt => validateUser(payload.password, userOpt))
  }

  private def validateUser(password: String, userOpt: Option[UserModelWithPassword]): EitherT[IO, Throwable, Option[UserModel]] = {
    userOpt match {
      case Some(user) if BCrypt.checkpw(password, user.password) =>
        EitherT.rightT(Some(UserModel.fromUserModelWithPassword(user)))
      case Some(_) =>
        EitherT.leftT(IncorrectUsernameOrPassword)
      case None =>
        EitherT.rightT(None)
    }
  }

  def signUpUser(payload: UserSignUpPayload): EitherT[IO, Throwable, UserModel] = {
    val hashedPassword = BCrypt.hashpw(payload.password, BCrypt.gensalt())
    val createUserPayload = CreateUserPayload(payload.username, hashedPassword)
    userRepository.createUser(createUserPayload)
  }

  def paginatedSearchUser(payload: PaginatedSearchUserPayload): EitherT[IO, Throwable, PaginatedSearchUserResponse] = {
    val searchPayload = PaginatedSearchUserPayloadRepo.fromPaginatedSearchUserPayload(payload)
    userRepository.paginatedSearchUser(searchPayload)
      .map(PaginatedSearchUserResponseRepo.toPaginatedSearchUserResponse)
  }
}

object UserService {
  def of(userRepository: UserRepository): UserService = new UserService(userRepository)
}