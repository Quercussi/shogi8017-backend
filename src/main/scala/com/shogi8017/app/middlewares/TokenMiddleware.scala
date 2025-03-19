package com.shogi8017.app.middlewares

import cats.effect.IO
import dev.profunktor.auth.jwt.{JwtAuth, JwtToken}
import io.circe.*
import io.circe.parser.*
import pdi.jwt.algorithms.JwtHmacAlgorithm
import pdi.jwt.{JwtAlgorithm, JwtClaim}

trait TokenMiddleware[T: Decoder, U] {
  def classMapper: T => U

  protected def authenticate: JwtToken => JwtClaim => IO[Option[U]] =
    (token: JwtToken) => (claim: JwtClaim) =>
      decode[T](claim.content) match {
        case Right(payload: T) =>
          IO.pure(Some(classMapper(payload)))
        case Left(err) =>
          IO.raiseError(new Exception(s"Failed to decode JWT claims: ${err.getMessage}"))
            .handleErrorWith { e =>
              IO(println(s"Authentication error: ${e.getMessage}")) *> IO.pure(None)
            }
      }
}