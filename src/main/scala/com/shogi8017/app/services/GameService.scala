package com.shogi8017.app.services

import cats.data.EitherT
import cats.effect.IO
import com.shogi8017.app.exceptions.GameNotFound
import com.shogi8017.app.models.GameModel
import com.shogi8017.app.repository.*
import com.shogi8017.app.routes.*
import com.shogi8017.app.services.GameActionService.{convertBoardHistoryToExecutionActionList, convertExecutionActionsToMoveResults}

class GameService(gameRepository: GameRepository, boardHistoryRepository: BoardHistoryRepository) {
  def paginatedGetGameByUserId(payload: PaginatedGetGameByUserIdPayload): EitherT[IO, Throwable, PaginatedGetGameByUserIdResponse] = {
    val searchPayload = PaginatedGetGameByUserIdPayloadRepo.fromPaginatedGetGameByUserIdPayload(payload)
    gameRepository.paginatedGetGameByUserId(searchPayload)
      .map(PaginatedGetGameByUserIdResponseRepo.toPaginatedGetGameByUserIdResponse)
  }

  def paginatedGetGameHistory(payload: PaginatedGetGameHistoryPayload): EitherT[IO, Throwable, PaginatedGetGameHistoryResponse] = {
    val queryPayloadGenerator = (boardId: String) => GetBoardHistoriesPaginatedPayload(boardId, payload.offset + payload.limit)
    for {
      game <- getGameEither(payload.gameCertificate)
      allBoardHistories <- boardHistoryRepository.getBoardHistoriesPaginated(queryPayloadGenerator(game.boardId))
      allExecutionActions <- EitherT.fromEither[IO](convertBoardHistoryToExecutionActionList(allBoardHistories.boardHistories))
      allMoveResults <- EitherT.fromEither[IO](convertExecutionActionsToMoveResults(allExecutionActions): Either[Throwable, List[MoveResultReduced]])

      totalCount = allBoardHistories.total
      (pagedResults, count, nextOffset) = paginateResults(allMoveResults, payload.offset, payload.limit, totalCount)
    } yield PaginatedGetGameHistoryResponse(pagedResults, count, nextOffset, totalCount)
  }

  private def getGameEither(gameCertificate: String): EitherT[IO, Throwable, GameModel] =
    gameRepository.getGame(GetGamePayload(gameCertificate))
      .subflatMap(_.toRight(GameNotFound))

  private def paginateResults[T](
    allResults: List[T],
    offset: Int,
    limit: Int,
    totalCount: Int
  ): (List[T], Int, Int) = {
    val endIndex = math.min(offset + limit, totalCount)

    val pagedResults = if (offset < totalCount) {
      allResults.slice(offset, endIndex)
    } else {
      List.empty[T]
    }

    val nextOffset = if (endIndex < totalCount) endIndex else -1

    (pagedResults, pagedResults.length, nextOffset)
  }
}

object GameService {
  def of(gameRepository: GameRepository, boardHistoryRepository: BoardHistoryRepository): GameService = new GameService(gameRepository, boardHistoryRepository)
}
