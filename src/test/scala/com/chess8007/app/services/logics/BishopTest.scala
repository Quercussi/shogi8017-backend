package com.chess8007.app.services.logics

import com.chess8007.app.errors.IllegalMove
import com.chess8007.app.services.logics.LogicTestUtils.*
import com.chess8007.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import org.scalatest.funsuite.AnyFunSuite

class BishopTest extends AnyFunSuite:
  test("A bishop should move diagonally") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Bishop(WHITE_PLAYER, true))
      + (Position(5,4) -> Bishop(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    val testingMoves1 = List((-3,-3),(4,4),(3,-3),(-3,3))
    testingMoves1.foreach((x, y) => testMove(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), Bishop(WHITE_PLAYER, true), newBoard1))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(4, 4), Bishop(WHITE_PLAYER, true), None)))
    val blackPos = Position(5,4)
    val testingMoves2 = List((-3,-3),(3,3),(3,-3),(-3,3))
    testingMoves2.foreach((x, y) => testMove(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(x, y)), Bishop(BLACK_PLAYER, true), newBoard2))
  }

  test("A bishop should not move like a cross") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Bishop(WHITE_PLAYER, true))
      + (Position(5, 4) -> Bishop(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(4, 8)), IllegalMove, newBoard1)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(4, 3)), IllegalMove, newBoard1)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(8, 4)), IllegalMove, newBoard1)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(2, 4)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(4, 4), Bishop(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 4), Position(7, 4)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 4), Position(3, 4)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 4), Position(5, 6)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 4), Position(5, 2)), IllegalMove, newBoard2)
  }

  test("A Bishop should capture diagonally") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Bishop(WHITE_PLAYER, true))
      + (Position(6, 2) -> Bishop(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMove(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(6, 2)), Bishop(WHITE_PLAYER, true), newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(4, 4), Bishop(WHITE_PLAYER, true), None)))
    testMove(BLACK_PLAYER, PlayerAction(Position(6, 2), Position(4, 4)), Bishop(BLACK_PLAYER, true), newBoard2)
  }

  test("A bishop should not capture like a cross") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Bishop(WHITE_PLAYER, true))
      + (Position(6, 4) -> Bishop(BLACK_PLAYER, true))
    
    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(6, 4)), IllegalMove, newBoard1)
    
    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(4, 4), Bishop(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(6, 4), Position(4, 4)), IllegalMove, newBoard2)
  }

  test("A Bishop should not jump pass another piece") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Bishop(WHITE_PLAYER, true))
      + (Position(8, 8) -> Bishop(BLACK_PLAYER, true))
      + (Position(6, 6) -> Rook(WHITE_PLAYER, true))
    
    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(7, 7)), IllegalMove, newBoard1)
    
    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(4, 4), Bishop(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(6, 6), Position(5, 5)), IllegalMove, newBoard2)
  }