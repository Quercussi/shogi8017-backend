package com.chess8007.app.repository

import cats.effect.IO
import doobie.util.transactor.Transactor


case class RepositoryCollection(userRepository: UserRepository, gameRepository: GameRepository)

object RepositoryCollection {
  def instantiateRepository(trx: Transactor[IO]): RepositoryCollection = {
    val userRepo: UserRepository = UserRepository.of(trx)
    val gameRepo: GameRepository = GameRepository.of(trx)
    RepositoryCollection(
      userRepository = userRepo,
      gameRepository = gameRepo,
    )
  }
}
