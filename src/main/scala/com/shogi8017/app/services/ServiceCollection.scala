package com.shogi8017.app.services

import com.shogi8017.app.AppConfig
import com.shogi8017.app.repository.RepositoryCollection

case class ServiceCollection(userService: UserService, gameService: GameService, authenticationService: AuthenticationService)

object ServiceCollection {
  def instantiateServices(appConfig: AppConfig, repositoryCollection: RepositoryCollection): ServiceCollection = {
    val userService = UserService.of(repositoryCollection.userRepository)
    val gameService = GameService.of(repositoryCollection.gameRepository, repositoryCollection.boardHistoryRepository)
    val authenticationService = AuthenticationService.of(appConfig.jwt, repositoryCollection.userRepository)
    ServiceCollection(
      userService = userService,
      gameService = gameService,
      authenticationService = authenticationService
    )
  }
}