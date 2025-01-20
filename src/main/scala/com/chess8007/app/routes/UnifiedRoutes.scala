package com.chess8007.app.routes

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import com.chess8007.app.{AppConfig, JwtConfig}
import com.chess8007.app.database.DatabaseResource
import com.chess8007.app.database.Database
import com.chess8007.app.repository.{RepositoryCollection, UserRepository}
import com.chess8007.app.services.{AuthenticationService, ServiceCollection, UserService}
import org.http4s.HttpRoutes

class UnifiedRoutes(authenticationRoutes: AuthenticationRoutesV2, unauthenticatedRoutes: UnauthenticatedRoutes) {
  def getRoutes: HttpRoutes[IO] = authenticationRoutes.getLoginRoute <+> unauthenticatedRoutes.getSignUpRoute
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

  private def instantiateRoutes(serviceCollection: ServiceCollection): UnifiedRoutes = {
    val authenticationRoutes = AuthenticationRoutesV2.of(serviceCollection.authenticationService)
    val unauthenticatedRoutes = UnauthenticatedRoutes.of(serviceCollection.userService)
    new UnifiedRoutes(authenticationRoutes, unauthenticatedRoutes)
  }

  def of(appConfig: AppConfig): UnifiedRoutes = {
    val db = instantiateDb(appConfig)
    val repositoryCollection = instantiateRepository(db)
    val serviceCollection = instantiateServices(appConfig, repositoryCollection)
    instantiateRoutes(serviceCollection)
  }
}
