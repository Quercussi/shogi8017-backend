package com.shogi8017.app.repository

import cats.effect.IO
import doobie.util.transactor.Transactor


case class RepositoryCollection(userRepository: UserRepository, gameRepository: GameRepository, invitationRepository: InvitationRepository, boardHistoryRepository: BoardHistoryRepository)

object RepositoryCollection {
  def instantiateRepository(trx: Transactor[IO]): RepositoryCollection = {
    val userRepo: UserRepository = UserRepository.of(trx)
    val gameRepo: GameRepository = GameRepository.of(trx)
    val invitationRepo: InvitationRepository = InvitationRepository.of(trx)
    val boardHistoryRepo: BoardHistoryRepository = BoardHistoryRepository.of(trx)
    RepositoryCollection(
      userRepository = userRepo,
      gameRepository = gameRepo,
      invitationRepository = invitationRepo,
      boardHistoryRepository = boardHistoryRepo
    )
  }
}
