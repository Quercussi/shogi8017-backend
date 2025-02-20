package com.shogi8017.app.middlewares

import cats.effect.IO
import com.shogi8017.app.JwtConfig
import com.shogi8017.app.jwtClaimModels.UserClaimModel
import com.shogi8017.app.middlewares.middlewareUtils.TokenDecoder.jwtAuthAccess
import com.shogi8017.app.models.UserModel
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s.server.AuthMiddleware

object AccessTokenMiddleware extends UserTokenMiddleware {
  def of(jwtConfig: JwtConfig): AuthMiddleware[IO, UserModel] = {
    JwtAuthMiddleware[IO, UserModel](
      jwtAuthAccess(jwtConfig.accessTokenSecret, jwtConfig.algorithm),
      authenticate
    )
  }
}
