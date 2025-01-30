package com.shogi8017.app.services.logics

import com.shogi8017.app.errors.{IllegalMove, NoPromotion}
import com.shogi8017.app.services.logics.Board.executeMove
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import org.scalatest.funsuite.AnyFunSuite

class PawnTest extends AnyFunSuite:
  test("Pawn should move forward by one square") {
    testMove(WHITE_PLAYER, PlayerAction(Position(2, 2), Position(2, 3), None), Pawn(WHITE_PLAYER, true))
  }

  test("Pawn should move forward by two squares from initial position") {
    testMove(WHITE_PLAYER, PlayerAction(Position(2, 2), Position(2, 4), None), Pawn(WHITE_PLAYER, true))
  }

  test("Pawn should not move forward by two squares if blocked as white") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.pieces
      - Position(3, 7)
      + (Position(3, 3) -> Pawn(Player.BLACK_PLAYER, true))
    val newBoard = Board(newPieces)

    testMoveError(WHITE_PLAYER, PlayerAction(Position(3, 2), Position(3, 4), None), IllegalMove, newBoard)
  }

  test("Pawn should not move forward by two squares if blocked as black") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.pieces
      - Position(3, 2)
      + (Position(3, 6) -> Pawn(Player.WHITE_PLAYER, true))
    val newBoard = Board(newPieces, Some(Move(WHITE_PLAYER, Position(3, 2), Position(3, 6), Pawn(WHITE_PLAYER, true), None)))

    testMoveError(BLACK_PLAYER, PlayerAction(Position(3, 7), Position(3, 5), None), IllegalMove, newBoard)
  }

  test("Pawn should not move forward by two squares if not in initial position") {
    val board0 = Board.defaultInitialPosition
    val board1 = testMove(WHITE_PLAYER, PlayerAction(Position(2, 2), Position(2, 3), None), Pawn(WHITE_PLAYER, true), board0)
    val board2 = testMove(BLACK_PLAYER, PlayerAction(Position(7, 7), Position(7, 6), None), Pawn(BLACK_PLAYER, true), board1)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(2, 3), Position(2, 5), None), IllegalMove, board2)
  }

  test("Pawn should not move backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER, true))
      + (Position(6, 6) -> Pawn(Player.BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(4, 3), None), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(4, 2), Position(4, 4), Pawn(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(6, 6), Position(4, 7), None), IllegalMove, newBoard2)
  }

  test("Pawn should not capture backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      - Position(4, 2)
      + (Position(4, 6) -> Pawn(Player.WHITE_PLAYER, true))
      - Position(3, 7)
      + (Position(3, 5) -> Pawn(Player.BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 6), Position(3, 5), None), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(4, 5), Position(4, 6), Pawn(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(3, 5), Position(4, 6), None), IllegalMove, newBoard2)
  }

  test("Pawn should not capture a piece of its own side") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER, true))
      + (Position(5, 5) -> Pawn(Player.WHITE_PLAYER, true))
      + (Position(4, 5) -> Pawn(Player.BLACK_PLAYER, true))
      + (Position(5, 4) -> Pawn(Player.BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(4, 5), None), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(4, 3), Position(4, 4), Pawn(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(4, 5), Position(5, 4), None), IllegalMove, newBoard2)
  }

  test("Pawn cannot move forward when blocked") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.pieces
      - Position(4, 2)
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER, true))
      - Position(4, 1)
      + (Position(4, 5) -> Queen(Player.WHITE_PLAYER, true))

    val newBoard = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(4, 5), None), IllegalMove, newBoard)
  }

  test("Pawn should not capture forward") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.pieces
      - Position(4, 2)
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER, true))
      - Position(4, 7)
      + (Position(4, 5) -> Pawn(Player.BLACK_PLAYER, true))

    val newBoard = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(4, 4), Position(4, 5), None), IllegalMove, newBoard)
  }

  test("Pawn should capture diagonally") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.pieces
      - Position(4, 7)
      + (Position(4, 5) -> Pawn(Player.BLACK_PLAYER, true))
      - Position(3, 2)
      + (Position(3, 4) -> Pawn(Player.WHITE_PLAYER, true))

    testMove(WHITE_PLAYER, PlayerAction(Position(3, 4), Position(4, 5), None), Pawn(Player.WHITE_PLAYER, true), Board(newPieces))
  }

  test("Pawn should be able to capture as en-passant as white") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.pieces
      - Position(3, 2)
      + (Position(3, 5) -> Pawn(Player.WHITE_PLAYER, true))

    val board0 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(3, 2), Position(3, 5), Pawn(WHITE_PLAYER, true), None)))
    val board1 = testMove(BLACK_PLAYER, PlayerAction(Position(4, 7), Position(4, 5), None), Pawn(Player.BLACK_PLAYER, true), board0)
    testMove(WHITE_PLAYER, PlayerAction(Position(3, 5), Position(4, 6), None), Pawn(Player.WHITE_PLAYER, true), board1)
  }

  test("Pawn should be able to capture as en-passant as black") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.pieces
      - Position(3, 7)
      + (Position(3, 4) -> Pawn(Player.BLACK_PLAYER, true))
    
    val board0 = Board(newPieces, Some(Move(BLACK_PLAYER, Position(3, 7), Position(3, 4), Pawn(BLACK_PLAYER, true), None)))
    val board1 = testMove(WHITE_PLAYER, PlayerAction(Position(4, 2), Position(4, 4), None), Pawn(WHITE_PLAYER, true), board0)
    testMove(BLACK_PLAYER, PlayerAction(Position(3, 4), Position(4, 3), None), Pawn(BLACK_PLAYER, true), board1)
  }

  test("Pawn should promote when reaching the last rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(1, 7) -> Pawn(WHITE_PLAYER, true))
      + (Position(1, 2) -> Pawn(BLACK_PLAYER, true))
    val board0 = Board(newPieces)

    val board1 = testMove(WHITE_PLAYER, PlayerAction(Position(1, 7), Position(1, 8), Some(Knight(WHITE_PLAYER))), Knight(WHITE_PLAYER, true), board0)
    testMove(BLACK_PLAYER, PlayerAction(Position(1, 2), Position(1, 1), Some(Bishop(BLACK_PLAYER))), Bishop(BLACK_PLAYER, true), board1)

    testMoveError(WHITE_PLAYER, PlayerAction(Position(1, 7), Position(1, 8), None), NoPromotion, board0)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(1, 2), Position(1, 1), None), NoPromotion, board1)
  }

  test("Pawn should not move diagonally without capturing") {
    val board = Board.defaultInitialPosition
    val playerAction = PlayerAction(Position(2, 2), Position(3, 3))
    val result = executeMove(board, WHITE_PLAYER, playerAction)
    assert(result.isInvalid)
  }