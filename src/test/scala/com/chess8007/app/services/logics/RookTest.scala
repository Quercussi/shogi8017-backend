package com.chess8007.app.services.logics

import com.chess8007.app.errors.IllegalMove
import com.chess8007.app.services.logics.LogicTestUtils.*
import com.chess8007.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import org.scalatest.funsuite.AnyFunSuite

class RookTest extends AnyFunSuite:
  test("A Rook should move like a cross") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Rook(WHITE_PLAYER, true))
      + (Position(3, 3) -> Rook(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    val testingMoves1 = List((3,0),(-3,0),(0,2),(0,-2))
    testingMoves1.foreach((x, y) => testMove(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), Rook(WHITE_PLAYER, true), newBoard1))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(4, 4), Rook(WHITE_PLAYER, true), None)))
    val blackPos = Position(3,3)
    val testingMoves2 = List((2,0),(-2,0),(0,3),(0,-2))
    testingMoves2.foreach((x, y) => testMove(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(x, y)), Rook(BLACK_PLAYER, true), newBoard2))
  }

  test("A rook should not move diagonally") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Rook(WHITE_PLAYER, true))
      + (Position(3, 3) -> Rook(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    val testingMoves1 = List((-3,-3),(4,4),(3,-3),(-3,3))
    testingMoves1.foreach((x, y) => testMoveError(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), IllegalMove, newBoard1))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(4, 4), Rook(WHITE_PLAYER, true), None)))
    val blackPos = Position(3,3)
    val testingMoves2 = List((-2,-2),(4,4),(4,-2),(-2,3))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(3, 3), Position(8, 8)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(3, 3), Position(1, 1)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(3, 3), Position(7, 1)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(3, 3), Position(1, 7)), IllegalMove, newBoard2)
  }

  test("A rook should capture like a cross") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Rook(WHITE_PLAYER, true))
      + (Position(6, 4) -> Rook(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMove(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(6, 4)), Rook(WHITE_PLAYER, true), newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(4, 4), Rook(WHITE_PLAYER, true), None)))
    testMove(BLACK_PLAYER, PlayerAction(Position(6, 4), Position(4, 4)), Rook(BLACK_PLAYER, true), newBoard2)
  }

  test("A rook should not capture diagonally") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Rook(WHITE_PLAYER, true))
      + (Position(2, 2) -> Rook(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(2, 2)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(4, 4), Rook(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(2, 2), Position(4, 4)), IllegalMove, newBoard2)
  }
  
  test("A should not jump pass another piece") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Rook(WHITE_PLAYER, true))
      + (Position(8, 4) -> Rook(BLACK_PLAYER, true))
      + (Position(6, 4) -> Rook(WHITE_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(7, 4)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(4, 4), Rook(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(8, 4), Position(5, 4)), IllegalMove, newBoard2)
  }