package com.shogi8017.app.services

import cats.data.{State, Validated}
import com.shogi8017.app.models.UserModel
import com.shogi8017.app.services.logics.{Board, Player, MoveAction, Position}
import com.shogi8017.app.errors.GameValidationError

case class GameState(board: Board, turnPlayer: Player, playerMap: Map[Player, UserModel], isValid: Boolean)

object GameState {
  def movePiece(playerAction: MoveAction): State[GameState, Unit] = State.modify { state =>
    val result = Board.executeMove(state.board, state.turnPlayer, playerAction)

    result match {
      case Validated.Valid((newBoard, _, _, _)) =>
        val nextPlayer = Player.opponent(state.turnPlayer)
        state.copy(board = newBoard, turnPlayer = nextPlayer, isValid = true)
      case Validated.Invalid(_) =>
        state.copy(isValid = false)
    }
  }
  
  def nextTurnPlayer: State[GameState, Unit] = State.modify { state =>
    state.copy(turnPlayer = Player.opponent(state.turnPlayer))
  }
}