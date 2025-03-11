package com.shogi8017.app.routes

import com.shogi8017.app.models.GameModel

case class PaginatedGetGameByUserIdPayload(userId: String, offset: Int, limit: Int)
case class PaginatedGetGameByUserIdResponse(games: List[GameModel], count: Int, nextOffset: Int, total: Int)
