package com.shogi8017.app.repository

import cats.effect.IO
import com.shogi8017.app.database.RedisResource
import doobie.util.transactor.Transactor


case class RepositoryCollection(userRepository: UserRepository, gameRepository: GameRepository, invitationRepository: InvitationRepository, boardHistoryRepository: BoardHistoryRepository, tokenRepository: TokenRepository)

object RepositoryCollection {
  def instantiateRepository(trx: Transactor[IO], redisResource: RedisResource): RepositoryCollection = {
    val userRepo: UserRepository = UserRepository.of(trx)
    val gameRepo: GameRepository = GameRepository.of(trx)
    val invitationRepo: InvitationRepository = InvitationRepository.of(trx)
    val boardHistoryRepo: BoardHistoryRepository = BoardHistoryRepository.of(trx)
    val tokenRepo: TokenRepository = TokenRepository.of(redisResource)
    RepositoryCollection(
      userRepository = userRepo,
      gameRepository = gameRepo,
      invitationRepository = invitationRepo,
      boardHistoryRepository = boardHistoryRepo,
      tokenRepository = tokenRepo
    )
  }
}
