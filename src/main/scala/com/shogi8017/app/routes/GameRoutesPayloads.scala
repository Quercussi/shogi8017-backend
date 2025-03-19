package com.shogi8017.app.routes

import com.shogi8017.app.models.{GameModel, UserModel}
import com.shogi8017.app.services.logics.pieces.PieceType
import com.shogi8017.app.services.logics.{AlgebraicNotation, GameEventWinnerPair, Player, Position, StateTransitionList}
import io.circe.Encoder
import io.circe.generic.semiauto.deriveEncoder

case class PaginatedGetGameByUserIdPayload(userId: String, offset: Int, limit: Int)
case class PaginatedGetGameByUserIdResponse(games: List[GameModel], count: Int, nextOffset: Int, total: Int)

case class PaginatedGetGameHistoryPayload(gameCertificate: String, offset: Int, limit: Int)
case class PaginatedGetGameHistoryResponse(executionHistories: List[MoveResultReduced], count: Int, nextOffset: Int, total: Int)

case class BoardConfiguration(board: List[PositionPiecePair], handPieceCounts: List[PieceHandCount])

case class MoveResultReduced(player: Player, stateTransitionList: StateTransitionList, algebraicNotation: AlgebraicNotation, gameEventWinnerPair: GameEventWinnerPair)

case class PlayerList(whitePlayer: UserModel, blackPlayer: UserModel)
object PlayerList {
  implicit val playerListEncoder: Encoder[PlayerList] = deriveEncoder
}

case class PositionPiecePair(position: Position, piece: PieceType, owner: Player)
object PositionPiecePair {
  implicit val positionPiecePairEncoder: Encoder[PositionPiecePair] = deriveEncoder
}

case class PieceHandCount(player: Player, piece: PieceType, count: Int)
object PieceHandCount {
  implicit val pieceHandCountEncoder: Encoder[PieceHandCount] = deriveEncoder
}