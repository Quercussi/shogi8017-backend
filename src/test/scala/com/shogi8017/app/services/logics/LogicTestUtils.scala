package com.shogi8017.app.services.logics

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import com.shogi8017.app.exceptions.GameValidationException
import com.shogi8017.app.services.logics.Board.executeOnBoardAction
import com.shogi8017.app.services.logics.actions.{DropAction, ExecutionAction, MoveAction, OnBoardAction}
import com.shogi8017.app.services.logics.pieces.Piece
import org.scalatest.Assertions.fail

object LogicTestUtils {
  def getAllPosition: Seq[Position] = {
    for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)
  }

  def testAction(player: Player, onBoardAction: OnBoardAction, expectedPiece: Piece, board: Board = Board.defaultInitialPosition): Board = {
    val result = executeOnBoardAction(board, player, onBoardAction)

    result match {
      case Valid((newBoard, _, _, _)) =>
        val targetPosition = onBoardAction match {
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

  def testActionError(player: Player, onBoardAction: OnBoardAction, expectedException: Exception, board: Board = Board.defaultInitialPosition): Unit = {
    val result = executeOnBoardAction(board, player, onBoardAction)

    result match {
      case Valid(moveResult) =>
        println(s"Move failed with result: $moveResult")
        fail("Move should be invalid")
      case Invalid(e) => assert(e == expectedException, s"Expected exception: $expectedException, but got: $e")
    }
  }

  def fromMovesList(moveList: List[ExecutionAction]): Validated[GameValidationException, Board] = {
    Board.fromExecutionList(moveList)
  }
}
