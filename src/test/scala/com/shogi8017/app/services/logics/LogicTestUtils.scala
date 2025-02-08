package com.shogi8017.app.services.logics

import cats.data.Validated.{Invalid, Valid}
import com.shogi8017.app.services.logics.Board.executeAction
import com.shogi8017.app.services.logics.pieces.Piece
import org.scalatest.Assertions.fail

object LogicTestUtils {
  def getAllPosition: Seq[Position] = {
    for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)
  }

  def testAction(player: Player, playerAction: PlayerAction, expectedPiece: Piece, board: Board = Board.defaultInitialPosition): Board = {
    val result = executeAction(board, player, playerAction)

    result match {
      case Valid((newBoard, _, _, _)) =>
        val targetPosition = playerAction match {
          case DropAction(position, _) => position
          case MoveAction(_, to, _)   => to
        }

        assert(newBoard.piecesMap.get(targetPosition).contains(expectedPiece))
        newBoard
      case invalid =>
        println(s"Move failed with result: $invalid")
        fail(s"Move should be valid. Expected result: $expectedPiece, but got: $invalid")
        board // This line will never be reached, but is required to satisfy the return type
    }
  }

  def testActionError(player: Player, playerAction: PlayerAction, expectedException: Exception, board: Board = Board.defaultInitialPosition): Unit = {
    val result = executeAction(board, player, playerAction)

    result match {
      case Valid(moveResult) =>
        println(s"Move failed with result: $moveResult")
        fail("Move should be invalid")
      case Invalid(e) => assert(e == expectedException, s"Expected exception: $expectedException, but got: $e")
    }
  }
}
