package com.shogi8017.app.services.logics

import cats.data.Validated.{Invalid, Valid}
import com.shogi8017.app.services.logics.Board.executeMove
import org.scalatest.Assertions.fail

object LogicTestUtils {
  def testMove(player: Player, playerAction: PlayerAction, expectedPiece: Piece, board: Board = Board.defaultInitialPosition): Board = {
    val result = executeMove(board, player, playerAction)

    result match {
      case Valid((newBoard, _, _, _)) =>
        assert(newBoard.pieces.get(playerAction.getFields._2).contains(expectedPiece))
        newBoard
      case invalid =>
        println(s"Move failed with result: $invalid")
        fail(s"Move should be valid. Expected result: $expectedPiece, but got: $invalid")
        board // This line will never be reached, but is required to satisfy the return type
    }
  }

  def testMoveError(player: Player, playerAction: PlayerAction, expectedException: Exception, board: Board = Board.defaultInitialPosition): Unit = {
    val result = executeMove(board, player, playerAction)

    result match {
      case Valid(moveResult) =>
        println(s"Move failed with result: $moveResult")
        fail("Move should be invalid")
      case Invalid(e) => assert(e == expectedException, s"Expected exception: $expectedException, but got: $e")
    }
  }
}
