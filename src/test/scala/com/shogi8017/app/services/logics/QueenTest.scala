package com.shogi8017.app.services.logics

import com.shogi8017.app.errors.IllegalMove
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import org.scalatest.funsuite.AnyFunSuite

class QueenTest extends AnyFunSuite:
  test("A queen should move like a star") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Queen(WHITE_PLAYER, true))
      + (Position(6, 5) -> Queen(BLACK_PLAYER, true))
    
    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    val testingMoves1 = List((3,0),(3,3),(0,3),(-1,1),(-2,0),(-2,-2),(0,-2),(2,-2))
    testingMoves1.foreach((x, y) => testMove(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), Queen(WHITE_PLAYER, true), newBoard1))
    
    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(4, 4), Queen(WHITE_PLAYER, true), None)))
    val blackPos = Position(6,5)
    val testingMoves2 = List((2,0),(2,2),(0,3),(-2,2),(-2,0),(-2,-2),(0,-2),(2,-2))
    testingMoves2.foreach((x, y) => testMove(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(x,y)), Queen(BLACK_PLAYER, true), newBoard2))
  }

  test("A queen should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Queen(WHITE_PLAYER, true))
      + (Position(6, 5) -> Queen(BLACK_PLAYER, true))
    
    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    val testingMoves1 = List((2,1),(2,3),(-1,-2),(-2,3),(-2,-1))
    testingMoves1.foreach((x, y) => testMoveError(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), IllegalMove, newBoard1))
    
    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(4, 4), Queen(WHITE_PLAYER, true), None)))
    val testingMoves2 = List((1,2),(1,3),(-2,-3),(-2,1),(-2,-4))
    val blackPos = Position(6,5)
    testingMoves2.foreach((x, y) => testMoveError(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(x, y)), IllegalMove, newBoard2))
  }
  
  test("A queen should capture like a star") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Queen(WHITE_PLAYER, true))
      + (Position(5, 4) -> Queen(WHITE_PLAYER, true))
      + (Position(4, 5) -> Queen(BLACK_PLAYER, true))
      + (Position(5, 5) -> Queen(BLACK_PLAYER, true))

    //  a b c d e f g h
    //8 . . . . k . . .
    //7 . . . . . . . .
    //6 . . . . . . . .
    //5 . . . q q . . .
    //4 . . . Q Q . . .
    //3 . . . . . . . .
    //2 . . . . . . . .
    //1 . . . . K . . .

    val newBoard1 = Board(newPieces)
    testMove(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(5, 5)), Queen(WHITE_PLAYER, true), newBoard1)
    testMove(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(4, 5)), Queen(WHITE_PLAYER, true), newBoard1)
    
    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(4, 4), Queen(WHITE_PLAYER, true), None)))
    testMove(BLACK_PLAYER, PlayerAction(Position(4, 5), Position(4, 4)), Queen(BLACK_PLAYER, true), newBoard2)
    testMove(BLACK_PLAYER, PlayerAction(Position(4, 5), Position(5, 4)), Queen(BLACK_PLAYER, true), newBoard2)
  }

  test("A queen should not capture a piece of its own side") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(5, 4) -> Queen(WHITE_PLAYER, true))
      + (Position(5, 5) -> Queen(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(5, 4), Position(5, 1)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 4), Position(5, 4), Queen(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 5), Position(5, 8)), IllegalMove, newBoard2)
  }

  test("A Queen should not jump pass a piece") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(1, 2) -> Queen(WHITE_PLAYER, true))
      + (Position(7, 2) -> Queen(BLACK_PLAYER, true))
      + (Position(3, 2) -> Pawn(WHITE_PLAYER, true))
      + (Position(4, 5) -> Pawn(WHITE_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(1, 2), Position(6, 2)), IllegalMove, newBoard1)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(1, 2), Position(6, 7)), IllegalMove, newBoard1)
    
    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 2), Position(4, 2), Queen(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(7, 2), Position(2, 2)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(7, 2), Position(2, 7)), IllegalMove, newBoard2)
  }