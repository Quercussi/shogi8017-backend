package com.chess8007.app.services.logics

import com.chess8007.app.errors.IllegalMove
import com.chess8007.app.services.logics.LogicTestUtils.*
import com.chess8007.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import org.scalatest.funsuite.AnyFunSuite

class KnightTest extends AnyFunSuite:
  test("A Knight should move diagonally") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Knight(WHITE_PLAYER, true))
      + (Position(5, 5) -> Knight(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    val testingMoves1 = List((2,1),(2,-1),(-2,1),(-2,-1),(1,2),(1,-2),(-1,2),(-1,-2))
    testingMoves1.foreach((x, y) => testMove(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), Knight(WHITE_PLAYER, true), newBoard1))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(4, 4), Knight(WHITE_PLAYER, true), None)))
    val blackPos = Position(5,5)
    val testingMoves2 = List((2,1),(2,-1),(-2,1),(-2,-1),(1,2),(1,-2),(-1,2),(-1,-2))
    testingMoves2.foreach((x, y) => testMove(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(x, y)), Knight(BLACK_PLAYER, true), newBoard2))
  }

  test("A bishop should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Knight(WHITE_PLAYER, true))
      + (Position(5, 5) -> Knight(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    val testingMoves1 = List((2,0),(2,3),(0,-2),(-2,3),(-2,-2))
    testingMoves1.foreach((x, y) => testMoveError(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), IllegalMove, newBoard1))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(3, 2), Position(4, 4), Knight(WHITE_PLAYER, true), None)))
    val blackPos = Position(5,5)
    val testingMoves2 = List((1,3),(1,3),(-2,-3),(-2,0),(-2,-4))
    testingMoves2.foreach((x, y) => testMoveError(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(x, y)), IllegalMove, newBoard2))
  }

  test("A knight should capture diagonally") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Knight(WHITE_PLAYER, true))
      + (Position(3, 2) -> Knight(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMove(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(3, 2)), Knight(WHITE_PLAYER, true), newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(6, 3), Position(4, 4), Knight(WHITE_PLAYER, true), None)))
    testMove(BLACK_PLAYER, PlayerAction(Position(3, 2), Position(4, 4)), Knight(BLACK_PLAYER, true), newBoard2)
  }

  test("A bishop should not capture like something else") {
    val emptyBoard = Board.emptyBoard
    val blackPawnPos = List(Position(4, 8), Position(4, 3), Position(8, 4), Position(2, 4))
    val blackPawns = blackPawnPos.map(pos => pos -> Pawn(BLACK_PLAYER, true))
    val whitePawnPos = List(Position(7, 5), Position(3, 5), Position(5, 7), Position(5, 3))
    val whitePawns = whitePawnPos.map(pos => pos -> Pawn(WHITE_PLAYER, true))
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Knight(WHITE_PLAYER, true))
      + (Position(5, 5) -> Knight(BLACK_PLAYER, true))
      ++ blackPawns ++ whitePawns

    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    blackPawnPos.foreach(pos => testMoveError(WHITE_PLAYER, PlayerAction(whitePos, pos), IllegalMove, newBoard1))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(4, 4), Knight(WHITE_PLAYER, true), None)))
    val blackPos = Position(5,5)
    whitePawnPos.foreach(pos => testMoveError(BLACK_PLAYER, PlayerAction(blackPos, pos), IllegalMove, newBoard2))
  }

  test("A knight should not capture a piece of its own side") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(6, 3) -> Knight(WHITE_PLAYER, true))
      + (Position(7, 7) -> Knight(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(6, 3), Position(5, 1), None), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(5, 1), Position(6, 3), Knight(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(7, 7), Position(5, 8), None), IllegalMove, newBoard2)
  }

  test("A knight should be able to jump") {
    val whitePos = Position(3, 3)
    val blackPos = Position(6, 6)
    val aroundMoves = for {
      dx <- -1 to 1
      dy <- -1 to 1
      if dx != 0 || dy != 0
    } yield (dx, dy)

    val whitePawnPos = aroundMoves.map((dx, dy) => whitePos.move(dx, dy))
    val blackPawnPos = aroundMoves.map((dx, dy) => blackPos.move(dx, dy))

    val whitePawns = whitePawnPos.map(pos => pos -> Pawn(WHITE_PLAYER, true))
    val blackPawns = blackPawnPos.map(pos => pos -> Pawn(BLACK_PLAYER, true))

    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      - Position(5, 1)
      + (Position(1, 1) -> King(WHITE_PLAYER, true))
      - Position(5, 8)
      + (Position(8, 8) -> King(BLACK_PLAYER, true))
      + (whitePos -> Knight(WHITE_PLAYER, true))
      + (blackPos -> Knight(BLACK_PLAYER, true))
      ++ whitePawns ++ blackPawns

    val newBoard1 = Board(newPieces)
    val testingMoves1 = List((2,1),(2,-1),(-2,1),(-2,-1),(1,2),(1,-2),(-1,2),(-1,-2))
    testingMoves1.foreach((dx, dy) => testMove(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(dx,dy)), Knight(WHITE_PLAYER, true), newBoard1))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 2), Position(3, 3), King(WHITE_PLAYER, true), None)))
    val testingMoves2 = List((2,1),(2,-1),(-2,1),(-2,-1),(1,2),(1,-2),(-1,2),(-1,-2))
    testingMoves2.foreach((dx, dy) => testMove(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(dx,dy)), Knight(BLACK_PLAYER, true), newBoard2))
  }

