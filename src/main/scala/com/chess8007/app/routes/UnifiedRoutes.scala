package com.chess8007.app.routes

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import com.chess8007.app.{AppConfig, JwtConfig}
import com.chess8007.app.database.DatabaseResource
import com.chess8007.app.database.Database
import org.http4s.HttpRoutes

class UnifiedRoutes(authenticationRoutes: AuthenticationRoutesV2, unauthenticatedRoutes: UnauthenticatedRoutes) {
  def getRoutes: HttpRoutes[IO] = authenticationRoutes.getLoginRoute <+> unauthenticatedRoutes.getSignUpRoute
}

object UnifiedRoutes {
  def of(appConfig: AppConfig): UnifiedRoutes = {
    val db: DatabaseResource = Database.of(appConfig).database
    val authenticationRoutes = AuthenticationRoutesV2.of(appConfig.jwt, db)
    val unauthenticatedRoutes = UnauthenticatedRoutes.of(db)
    new UnifiedRoutes(authenticationRoutes, unauthenticatedRoutes)
  }
}
