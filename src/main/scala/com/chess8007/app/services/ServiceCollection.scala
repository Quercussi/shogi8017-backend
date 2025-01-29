package com.chess8007.app.services

import com.chess8007.app.AppConfig
import com.chess8007.app.repository.RepositoryCollection

case class ServiceCollection(userService: UserService, authenticationService: AuthenticationService)

object ServiceCollection {
  def instantiateServices(appConfig: AppConfig, repositoryCollection: RepositoryCollection): ServiceCollection = {
    val userService = UserService.of(repositoryCollection.userRepository)
    val authenticationService = AuthenticationService.of(appConfig.jwt, repositoryCollection.userRepository)
    ServiceCollection(
      userService = userService,
      authenticationService = authenticationService
    )
  }
}