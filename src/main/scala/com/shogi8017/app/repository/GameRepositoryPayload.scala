package com.shogi8017.app.repository

import com.shogi8017.app.models.GameModel
import com.shogi8017.app.routes.{PaginatedGetGameByUserIdPayload, PaginatedGetGameByUserIdResponse}

case class CreateGamePayload(gameCertificate: String, whiteUserId: String, blackUserId: String)

case class GetGamePayload(gameCertificate: String)

case class PaginatedGetGameByUserIdPayloadRepo(userId: String, offset: Int, limit: Int)
case class PaginatedGetGameByUserIdResponseRepo(games: List[GameModel], nextOffset: Int, total: Int)


object PaginatedGetGameByUserIdPayloadRepo {
  def fromPaginatedGetGameByUserIdPayload(payload: PaginatedGetGameByUserIdPayload): PaginatedGetGameByUserIdPayloadRepo = {
    PaginatedGetGameByUserIdPayloadRepo(payload.userId, payload.offset, payload.limit)
  }
}

object PaginatedGetGameByUserIdResponseRepo {
  def toPaginatedGetGameByUserIdResponse(payload: PaginatedGetGameByUserIdResponseRepo): PaginatedGetGameByUserIdResponse = {
    PaginatedGetGameByUserIdResponse(payload.games, payload.games.length, payload.nextOffset, payload.total)
  }
}