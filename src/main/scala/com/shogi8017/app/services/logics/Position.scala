package com.shogi8017.app.services.logics

import com.shogi8017.app.services.logics.Board.*

case class Position(x: Int, y: Int) {
  /**
   * Determines if this position is under attack by the opponent's pieces.
   *
   * @param board  The current state of the Shogi board.
   * @param player The player whose position are being protected.
   * @return `true` if this position is under attack by any of the opponent's pieces, `false` otherwise.
   */
  def isUnderAttack(board: Board, player: Player): Boolean = {
    val opponent = if player == Player.WHITE_PLAYER then Player.BLACK_PLAYER else Player.WHITE_PLAYER
    existsPlayerPieces(board,opponent) { (currentPosition, piece) =>
      piece.getBoardTransitionOnMove(board, MoveAction(currentPosition, this)).isValid
    }
  }

  /**
   * Checks if the position is outside the valid board boundaries.
   *
   * @return `true` if the position is out of bounds, otherwise `false`.
   */
  def isOutOfBoard: Boolean = {
    x < 1 || x > 9 || y < 1 || y > 9
  }

  /**
   * Moves the position by a given offset.
   *
   * @param dir The direction to move the position.
   * @return A new `Position` instance with the updated coordinates.
   */
  def move(dir: Direction): Position = Position(x + dir.dx, y + dir.dy)
}

object Position {
  def sentinelPosition: Position = Position(-1, -1)
}