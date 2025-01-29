package com.chess8007.app.repository

case class RepositoryCollection(userRepository: UserRepository)
import cats.effect.IO
import doobie.util.transactor.Transactor


case class RepositoryCollection(userRepository: UserRepository, gameRepository: GameRepository)

object RepositoryCollection {
  def instantiateRepository(trx: Transactor[IO]): RepositoryCollection = {
    val userRepo: UserRepository = UserRepository.of(trx)
    RepositoryCollection(
      userRepository = userRepo,
    )
  }
}
