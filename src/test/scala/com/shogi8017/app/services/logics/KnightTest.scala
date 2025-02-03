package com.shogi8017.app.services.logics

import com.shogi8017.app.errors.{ExpectingPromotion, IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.{Knight, Pawn, PromotedKnight}
import org.scalatest.funsuite.AnyFunSuite

class KnightTest extends AnyFunSuite:
  test("Knight should move like a knight") {
    val defaultBoard = Board.defaultInitialPosition
    val newPiece = defaultBoard.piecesMap
      - Position(3, 3)
      - Position(3, 7)

    val s0 = Board(newPiece)
    val s1 = testMove(WHITE_PLAYER, MoveAction(Position(2, 1), Position(3, 3)), Knight(WHITE_PLAYER), s0)
    testMove(BLACK_PLAYER, MoveAction(Position(2, 9), Position(3, 7)), Knight(BLACK_PLAYER), s1)
  }

  test("Knight should not move backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Knight(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(3, 2)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(7, 8)), IllegalMove, newBoard2)
  }

  test("Knight should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Knight(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(5, 7)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(2, 4)), IllegalMove, newBoard2)
  }

  test("Knight should be able to jump") {
    val defaultBoard = Board.emptyBoard
    val newPieces = defaultBoard.piecesMap
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      + (Position(4, 5) -> Pawn(Player.WHITE_PLAYER))
      + (Position(5, 5) -> Pawn(Player.WHITE_PLAYER))
      + (Position(5, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(5, 3) -> Pawn(Player.WHITE_PLAYER))
      + (Position(4, 3) -> Pawn(Player.WHITE_PLAYER))
      + (Position(3, 3) -> Pawn(Player.WHITE_PLAYER))
      + (Position(3, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(3, 5) -> Pawn(Player.WHITE_PLAYER))

    val newBoard = Board(newPieces)
    testMove(WHITE_PLAYER, MoveAction(Position(4, 4), Position(5, 6)), Knight(WHITE_PLAYER), newBoard)
  }

  test("Knight should capture forward") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.piecesMap
      - Position(4, 2)
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      - Position(4, 7)
      + (Position(3, 6) -> Knight(Player.BLACK_PLAYER))

    val newBoard = Board(newPieces)
    testMove(WHITE_PLAYER, MoveAction(Position(4, 4), Position(3, 6)), Knight(Player.WHITE_PLAYER), newBoard)
  }

  test("Knight should not capture a piece of its own side") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      + (Position(3, 6) -> Knight(Player.WHITE_PLAYER))
      + (Position(5, 5) -> Knight(Player.BLACK_PLAYER))
      + (Position(6, 3) -> Knight(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(3, 6)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(5, 5), Position(6, 3)), IllegalMove, newBoard2)
  }

  test("Knight should not capture backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(3, 6) -> Knight(Player.WHITE_PLAYER))
      + (Position(4, 4) -> Knight(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(3, 6), Position(4, 4)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(4, 4), Position(3, 6)), IllegalMove, newBoard2)
  }

  test("Knight should promote when reaching the second last rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(2, 6) -> Knight(WHITE_PLAYER))
      + (Position(5, 4) -> Knight(BLACK_PLAYER))
    val board0 = Board(newPieces)

    testMoveError(WHITE_PLAYER, MoveAction(Position(2, 6), Position(1, 8), false), ExpectingPromotion, board0)
    val board1 = testMove(WHITE_PLAYER, MoveAction(Position(2, 6), Position(1, 8), true), PromotedKnight(WHITE_PLAYER), board0)

    testMoveError(BLACK_PLAYER, MoveAction(Position(5, 4), Position(4, 2), false), ExpectingPromotion, board1)
    testMove(BLACK_PLAYER, MoveAction(Position(5, 4), Position(4, 2), true), PromotedKnight(BLACK_PLAYER), board1)
  }

  test("Knight cannot promote outside the last three rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(1, 4) -> Knight(WHITE_PLAYER))
      + (Position(2, 6) -> Knight(BLACK_PLAYER))
    val board0 = Board(newPieces)
    val board1 = board0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(1, 4), Position(2, 6), true), IncorrectPromotionScenario, board0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(2, 6), Position(3, 4), true), IncorrectPromotionScenario, board1)
  }
  
  // TODO: test drop actions