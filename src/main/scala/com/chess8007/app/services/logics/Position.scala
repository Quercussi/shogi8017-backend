package com.shogi8017.app.services.logics

import com.shogi8017.app.services.logics.Board.*
import com.shogi8017.app.services.logics.PositionColor

case class Position(x: Int, y: Int) {
  /**
   * Determines if this position is under attack by the opponent's pieces.
   *
   * @param board  The current state of the chess board.
   * @param player The player whose position are being protected.
   * @return `true` if this position is under attack by any of the opponent's pieces, `false` otherwise.
   */
  def isUnderAttack(board: Board, player: Player): Boolean = {
    val opponent = if player == Player.WHITE_PLAYER then Player.BLACK_PLAYER else Player.WHITE_PLAYER
    val k = existsPlayerPieces(board,opponent) { (currentPosition, piece) =>
      val k = piece.getBoardTransition(board, PlayerAction(currentPosition, this)).isValid
      k
    }
    k
  }

  /**
   * Determines the color of the position on the board.
   *
   * @return The position color (`BLACK_POSITION` or `WHITE_POSITION`).
   */
  def getPositionColor: PositionColor = {
    if (x + y) % 2 == 0 then
      PositionColor.BLACK_POSITION
    else
      PositionColor.WHITE_POSITION
  }

  /**
   * Checks if the position is outside the valid board boundaries.
   *
   * @return `true` if the position is out of bounds, otherwise `false`.
   */
  def isOutOfBoard: Boolean = {
    x < 1 || x > 8 || y < 1 || y > 8
  }

  /**
   * Moves the position by a given offset.
   *
   * @param dx The offset in the x-direction.
   * @param dy The offset in the y-direction.
   * @return A new `Position` instance with the updated coordinates.
   */
  def move(dx: Int, dy: Int): Position = Position(x + dx, y + dy)
}