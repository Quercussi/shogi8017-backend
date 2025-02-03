package com.shogi8017.app.services.logics

import com.shogi8017.app.errors.{ExpectingPromotion, IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.{Gold, Pawn, PromotedPawn}
import org.scalatest.funsuite.AnyFunSuite

class PawnTest extends AnyFunSuite:
  test("Pawn should move forward by one square") {
    val s1 = testMove(WHITE_PLAYER, MoveAction(Position(2, 3), Position(2, 4)), Pawn(WHITE_PLAYER))
    testMove(BLACK_PLAYER, MoveAction(Position(2, 7), Position(2, 6)), Pawn(BLACK_PLAYER), s1)
  }

  test("Pawn should not move backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Pawn(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 3)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(4, 7)), IllegalMove, newBoard2)
  }

  test("Pawn should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Pawn(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(5, 6)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(2, 4)), IllegalMove, newBoard2)
  }

  test("Pawn cannot move forward when blocked") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.piecesMap
      - Position(4, 2)
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      - Position(4, 1)
      + (Position(4, 5) -> Gold(Player.WHITE_PLAYER))

    val newBoard = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 5)), IllegalMove, newBoard)
  }

  test("Pawn should capture forward") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.piecesMap
      - Position(4, 2)
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      - Position(4, 7)
      + (Position(4, 5) -> Pawn(Player.BLACK_PLAYER))

    val newBoard = Board(newPieces)
    testMove(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 5)), Pawn(Player.WHITE_PLAYER), newBoard)
  }

  test("Pawn should not capture a piece of its own side") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(4, 5) -> Pawn(Player.WHITE_PLAYER))
      + (Position(5, 5) -> Pawn(Player.BLACK_PLAYER))
      + (Position(5, 4) -> Pawn(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 5)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(5, 5), Position(5, 4)), IllegalMove, newBoard2)
  }

  test("Pawn should not capture backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      - Position(4, 2)
      + (Position(4, 6) -> Pawn(Player.WHITE_PLAYER))
      - Position(3, 7)
      + (Position(3, 5) -> Pawn(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 6), Position(3, 5)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(3, 5), Position(4, 6)), IllegalMove, newBoard2)
  }

  test("Pawn should promote when reaching the last rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(1, 8) -> Pawn(WHITE_PLAYER))
      + (Position(1, 2) -> Pawn(BLACK_PLAYER))
    val board0 = Board(newPieces)

    testMoveError(WHITE_PLAYER, MoveAction(Position(1, 8), Position(1, 9), false), ExpectingPromotion, board0)
    val board1 = testMove(WHITE_PLAYER, MoveAction(Position(1, 8), Position(1, 9), true), PromotedPawn(WHITE_PLAYER), board0)

    testMoveError(BLACK_PLAYER, MoveAction(Position(1, 2), Position(1, 1), false), ExpectingPromotion, board1)
    testMove(BLACK_PLAYER, MoveAction(Position(1, 2), Position(1, 1), true), PromotedPawn(BLACK_PLAYER), board1)
  }

  test("Pawn cannot promote outside the last three rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(1, 5) -> Pawn(WHITE_PLAYER))
      + (Position(2, 5) -> Pawn(BLACK_PLAYER))
    val board0 = Board(newPieces)
    val board1 = board0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(1, 5), Position(1, 6), true), IncorrectPromotionScenario, board0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(2, 5), Position(2, 4), true), IncorrectPromotionScenario, board1)
  }
  
  // TODO: test drop actions