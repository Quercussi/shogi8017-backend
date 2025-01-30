package com.shogi8017.app.services

import cats.data.State
import com.shogi8017.app.services.logics.{Board, Player, Position}

case class GameState(board: Board, turnUser: User, playerMap: Map[Player, User])

type Game[A] = State[GameState, A]

def movePiece(from: Position, to: Position): Game[Unit] = State.modify {
  state => state.copy(board = state.board.move(from, to), turn = state.turn.next);
}

def currentTurn: Game[User] = State.inspect(_.turn)