package com.shogi8017.app.services

import cats.data.EitherT
import cats.effect.IO
import com.shogi8017.app.repository.{GameRepository, PaginatedGetGameByUserIdPayloadRepo, PaginatedGetGameByUserIdResponseRepo}
import com.shogi8017.app.routes.{PaginatedGetGameByUserIdPayload, PaginatedGetGameByUserIdResponse}

class GameService(gameRepository: GameRepository) {
  def paginatedGetGameByUserId(payload: PaginatedGetGameByUserIdPayload): EitherT[IO, Throwable, PaginatedGetGameByUserIdResponse] = {
    val searchPayload = PaginatedGetGameByUserIdPayloadRepo.fromPaginatedGetGameByUserIdPayload(payload)
    gameRepository.paginatedGetGameByUserId(searchPayload)
      .map(PaginatedGetGameByUserIdResponseRepo.toPaginatedGetGameByUserIdResponse)
  }
}

object GameService {
  def of(gameRepository: GameRepository): GameService = new GameService(gameRepository) 
}
