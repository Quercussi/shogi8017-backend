package com.shogi8017.app.services

import com.shogi8017.app.AppConfig
import com.shogi8017.app.repository.RepositoryCollection

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