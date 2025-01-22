package com.chess8007.app.routes

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import com.chess8007.app.{AppConfig, JwtConfig}
import com.chess8007.app.database.DatabaseResource
import com.chess8007.app.database.Database
import com.chess8007.app.middlewares.*
import com.chess8007.app.repository.{RepositoryCollection, UserRepository}
import com.chess8007.app.services.{AuthenticationService, ServiceCollection, UserService}
import org.http4s.HttpRoutes

class UnifiedRoutes(mc: MiddlewareCollection, authenticationRoutes: AuthenticationRoutes, unauthenticatedRoutes: UnauthenticatedRoutes) {
  def getRoutes: HttpRoutes[IO] = authenticationRoutes.getLoginRoute 
    <+> mc.refreshTokenMiddleware(authenticationRoutes.getRefreshTokenRoute)
    <+> unauthenticatedRoutes.getSignUpRoute 
}

object UnifiedRoutes {
  private def instantiateDb(appConfig: AppConfig): DatabaseResource = Database.of(appConfig).database

  private def instantiateRepository(db: DatabaseResource): RepositoryCollection = {
    val userRepo: UserRepository = UserRepository.of(db)
    RepositoryCollection(
      userRepository = userRepo
    )
  }

  private def instantiateServices(appConfig: AppConfig, repositoryCollection: RepositoryCollection): ServiceCollection = {
    val userService = UserService.of(repositoryCollection.userRepository)
    val authenticationService = AuthenticationService.of(appConfig.jwt, repositoryCollection.userRepository)
    ServiceCollection(
      userService = userService,
      authenticationService = authenticationService
    )
  }

  private def instantiateMiddlewares(jwtConfig: JwtConfig): MiddlewareCollection = {
    val accessTokenMiddleware = AccessTokenMiddleware.of(jwtConfig)
    val refreshTokenMiddleware = RefreshTokenMiddleware.of(jwtConfig)
    MiddlewareCollection(
      accessTokenMiddleware = accessTokenMiddleware,
      refreshTokenMiddleware = refreshTokenMiddleware
    )
  }
  
  private def instantiateRoutes(middlewareCollection: MiddlewareCollection, serviceCollection: ServiceCollection): UnifiedRoutes = {
    val authenticationRoutes = AuthenticationRoutes.of(serviceCollection.authenticationService)
    val unauthenticatedRoutes = UnauthenticatedRoutes.of(serviceCollection.userService)
    new UnifiedRoutes(middlewareCollection, authenticationRoutes, unauthenticatedRoutes)
  }
  
  def of(appConfig: AppConfig): UnifiedRoutes = {
    val db = instantiateDb(appConfig)
    val repositoryCollection = instantiateRepository(db)
    val serviceCollection = instantiateServices(appConfig, repositoryCollection)
    val middlewareCollection = instantiateMiddlewares(appConfig.jwt)
    instantiateRoutes(middlewareCollection, serviceCollection)
  }
}
