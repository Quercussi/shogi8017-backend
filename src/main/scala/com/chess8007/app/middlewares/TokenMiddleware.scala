package com.chess8007.app.middlewares

import cats.effect.IO
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken}
import io.circe.*
import io.circe.parser.*
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtClaim}

trait TokenMiddleware[T: Decoder, U] {
  def classMapper: T => U
  
  protected def authenticate: JwtToken => JwtClaim => IO[Option[U]] =
    (token: JwtToken) => (claim: JwtClaim) => decode[T](claim.content) match {
      case Right(payload: T) => IO.pure(Some(classMapper(payload)))
      case Left(err) => IO(println(s"Failed to decode JWT claims: ${err.getMessage}")) *> IO.pure(None)
    }

  protected def jwtAlgorithm(algorithm: Option[String]): JwtHmacAlgorithm = JwtAlgorithm.fromString(algorithm.getOrElse("HS256")) match {
    case algo: JwtHmacAlgorithm => algo
    case _ => throw new IllegalArgumentException("The provided algorithm is not a HMAC algorithm")
  }

  protected def jwtAuthAccess(tokenSecret: String, algorithm: Option[String]): JwtAuth = JwtAuth.hmac(tokenSecret.toCharArray, jwtAlgorithm(algorithm))
}