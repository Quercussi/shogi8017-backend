package com.shogi8017.app.middlewares.middlewareUtils

import cats.data.EitherT
import cats.effect.IO
import cats.implicits.catsSyntaxEither
import com.shogi8017.app.jwtClaimModels.UserClaimModel
import com.shogi8017.app.models.UserModel
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken, jwtDecode}
import io.circe.parser.decode
import pdi.jwt.JwtAlgorithm
import pdi.jwt.algorithms.JwtHmacAlgorithm

object TokenDecoder {
  def decodeUserToken(
   token: String,
   secret: String,
   algorithm: Option[String],
   classMapper: UserClaimModel => UserModel,
   payloadError: String,
   decodeError: String => String
  ): EitherT[IO, String, UserModel] = {
    val jwtAuth = jwtAuthAccess(secret, algorithm)
    for {
      claim <- EitherT.liftF(jwtDecode[IO](JwtToken(token), jwtAuth))
      userClaim <- EitherT.fromEither(decode[UserClaimModel](claim.content).leftMap(_ => decodeError(payloadError)))
    } yield classMapper(userClaim)
  }

  def jwtAuthAccess(tokenSecret: String, algorithm: Option[String]): JwtAuth = JwtAuth.hmac(tokenSecret.toCharArray, jwtAlgorithm(algorithm))

  def jwtAlgorithm(algorithm: Option[String]): JwtHmacAlgorithm = {
    JwtAlgorithm.fromString(algorithm.getOrElse("HS256")) match {
      case algo: JwtHmacAlgorithm => algo
      case invalid =>
        throw new IllegalArgumentException(s"Invalid algorithm: $invalid. Supported: HMAC algorithms only.")
    }
  }
}
