package com.chess8007.app.services.logics

import cats.data.Validated.{Valid, Invalid}
import com.chess8007.app.errors.IllegalMove
import com.chess8007.app.services.logics.Board.{executeMove, fromMovesList}
import com.chess8007.app.services.logics.GameEvent.*
import com.chess8007.app.services.logics.LogicTestUtils.{testMove, testMoveError}
import com.chess8007.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import org.scalatest.funsuite.AnyFunSuite

class BoardTest extends AnyFunSuite {

  test("defaultInitialPosition should initialize the board correctly") {
    val board = Board.defaultInitialPosition

    // Check the white pieces
    assert(board.pieces.get(Position(1, 1)).contains(Rook(WHITE_PLAYER)))
    assert(board.pieces.get(Position(2, 1)).contains(Knight(WHITE_PLAYER)))
    assert(board.pieces.get(Position(3, 1)).contains(Bishop(WHITE_PLAYER)))
    assert(board.pieces.get(Position(4, 1)).contains(Queen(WHITE_PLAYER)))
    assert(board.pieces.get(Position(5, 1)).contains(King(WHITE_PLAYER)))
    assert(board.pieces.get(Position(6, 1)).contains(Bishop(WHITE_PLAYER)))
    assert(board.pieces.get(Position(7, 1)).contains(Knight(WHITE_PLAYER)))
    assert(board.pieces.get(Position(8, 1)).contains(Rook(WHITE_PLAYER)))
    (1 to 8).foreach(col => assert(board.pieces.get(Position(col, 2)).contains(Pawn(WHITE_PLAYER))))

    // Check the black pieces
    assert(board.pieces.get(Position(1, 8)).contains(Rook(BLACK_PLAYER)))
    assert(board.pieces.get(Position(2, 8)).contains(Knight(BLACK_PLAYER)))
    assert(board.pieces.get(Position(3, 8)).contains(Bishop(BLACK_PLAYER)))
    assert(board.pieces.get(Position(4, 8)).contains(Queen(BLACK_PLAYER)))
    assert(board.pieces.get(Position(5, 8)).contains(King(BLACK_PLAYER)))
    assert(board.pieces.get(Position(6, 8)).contains(Bishop(BLACK_PLAYER)))
    assert(board.pieces.get(Position(7, 8)).contains(Knight(BLACK_PLAYER)))
    assert(board.pieces.get(Position(8, 8)).contains(Rook(BLACK_PLAYER)))
    (1 to 8).foreach(col => assert(board.pieces.get(Position(col, 7)).contains(Pawn(BLACK_PLAYER))))
  }

  test("emptyPosition should initialize the empty board correctly") {
    val board = Board.emptyBoard

    assert(board.pieces.get(Position(5, 1)).contains(King(WHITE_PLAYER)))
    assert(board.pieces.get(Position(5, 8)).contains(King(BLACK_PLAYER)))
  }

  test("board should recognize a stalemate") {
    val board = Board.emptyBoard
    val newPieces = board.pieces
      - Position(5,1)
      + (Position(1,2) -> King(WHITE_PLAYER))
      + (Position(2,8) -> Rook(BLACK_PLAYER))
      + (Position(8,8) -> Rook(BLACK_PLAYER))

    val board0 = Board(newPieces)
    val board1 = testMove(WHITE_PLAYER, PlayerAction(Position(1, 2), Position(1, 1), None), King(WHITE_PLAYER, true), board0)

    executeMove(board1, BLACK_PLAYER, PlayerAction(Position(8, 8), Position(8, 2), None)) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent match {
          case Some(STALEMATE) => ()
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("board should recognize a dead position (king-vs-king)") {
    val board = Board.emptyBoard
    val newPieces = board.pieces
      + (Position(4,1) -> Queen(BLACK_PLAYER))

    val board0 = Board(newPieces)
    val board1 = testMove(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(4, 1), None), King(WHITE_PLAYER, true), board0)

    executeMove(board1, BLACK_PLAYER, PlayerAction(Position(5, 8), Position(4, 8), None)) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent match {
          case Some(DEAD_POSITION) => ()
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("board should recognize a dead position (king-vs-king-n-bishop)") {
    val board = Board.emptyBoard
    val newPieces = board.pieces
      + (Position(4, 2) -> Bishop(BLACK_PLAYER))
      + (Position(4, 1) -> Queen(BLACK_PLAYER))

    val board0 = Board(newPieces)
    val board1 = testMove(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(4, 1), None), King(WHITE_PLAYER, true), board0)

    executeMove(board1, BLACK_PLAYER, PlayerAction(Position(5, 8), Position(4, 8), None)) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent match {
          case Some(DEAD_POSITION) => ()
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("board should recognize a dead position (king-n-bishop-vs-king-n-bishop)") {
    val board = Board.emptyBoard
    val newPieces = board.pieces
      + (Position(4, 2) -> Bishop(BLACK_PLAYER))
      + (Position(4, 1) -> Queen(BLACK_PLAYER))
      + (Position(1, 1) -> Bishop(WHITE_PLAYER))

    val board0 = Board(newPieces)
    val board1 = testMove(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(4, 1), None), King(WHITE_PLAYER, true), board0)

    executeMove(board1, BLACK_PLAYER, PlayerAction(Position(5, 8), Position(4, 8), None)) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent match {
          case Some(DEAD_POSITION) => ()
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("board should recognize a check") {
    val board = Board.emptyBoard
    val newPieces = board.pieces
      - Position(5, 1)
      + (Position(1, 2) -> King(WHITE_PLAYER))
      + (Position(3,4) -> Pawn(WHITE_PLAYER))
      + (Position(2, 8) -> Rook(BLACK_PLAYER))
      + (Position(8, 8) -> Rook(BLACK_PLAYER))

    val board0 = Board(newPieces)
    val board1 = testMove(WHITE_PLAYER, PlayerAction(Position(1, 2), Position(1, 1), None), King(WHITE_PLAYER, true), board0)

    executeMove(board1, BLACK_PLAYER, PlayerAction(Position(8, 8), Position(8, 1), None)) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent match {
          case Some(CHECK) => ()
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("Board should recognize a checkmate") {
    val board = Board.emptyBoard
    val newPieces = board.pieces
      - Position(5,1)
      + (Position(1,1) -> King(WHITE_PLAYER))
      + (Position(3,4) -> Pawn(WHITE_PLAYER))
      + (Position(2,8) -> Rook(BLACK_PLAYER))
      + (Position(7,2) -> Rook(BLACK_PLAYER))
      + (Position(8,8) -> Rook(BLACK_PLAYER))

    //  a b c d e f g h
    //8 . r . . K . . r
    //7 . . . . . . . .
    //6 . . . . . . . .
    //5 . . . . . . . .
    //4 . . P . . . . .
    //3 . . . . . . . .
    //2 . . . . . . r .
    //1 K . . . . . . .

    val board0 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(2, 1), Position(1, 1), King(WHITE_PLAYER, true), None)))

    executeMove(board0, BLACK_PLAYER, PlayerAction(Position(8, 8), Position(8, 1), None)) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent match {
          case Some(CHECKMATE) => ()
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }
  
  test("A player should not move that check themself") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(6, 8) -> Rook(WHITE_PLAYER, true))
      + (Position(8, 8) -> Rook(BLACK_PLAYER, true))

    testMoveError(WHITE_PLAYER, PlayerAction(Position(8, 8), Position(8, 7), None), IllegalMove, Board(newPieces))
  }

  test("False-Checkmate test 1") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      -  Position(5, 1)
      + (Position(1, 1) -> King(WHITE_PLAYER))
      + (Position(2, 7) -> Rook(WHITE_PLAYER))
      + (Position(7, 1) -> Rook(BLACK_PLAYER))
      + (Position(8, 8) -> Rook(BLACK_PLAYER))

      //a b c d e f g h
      //8 . . . k . . . r
      //7 . R . . . . . .
      //6 . . . . . . . .
      //5 . . . . . . . .
      //4 . . . . . . . .
      //3 . . . . . . . .
      //2 . . . . . . . .
      //1 K . . . . . r .

    val board0 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(2, 1), Position(1, 1), King(WHITE_PLAYER, true), None)))
    executeMove(board0, BLACK_PLAYER, PlayerAction(Position(8, 8), Position(8, 1), None)) match {
      case Valid((board1, _, _, gameEvent)) =>
        gameEvent match {
          case Some(CHECK) => testMove(WHITE_PLAYER, PlayerAction(Position(2, 7), Position(2, 1), None), Rook(WHITE_PLAYER, true), board1)
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("False-Checkmate test 2") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      - Position(5, 1)
      + (Position(1, 1) -> King(WHITE_PLAYER))
      + (Position(3, 3) -> Knight(WHITE_PLAYER))
      + (Position(7, 1) -> Rook(BLACK_PLAYER))
      + (Position(8, 8) -> Rook(BLACK_PLAYER))

    //a b c d e f g h
    //8 . . . k . . . r
    //7 . . . . . . . .
    //6 . . . . . . . .
    //5 . . . . . . . .
    //4 . . . . . . . .
    //3 . . N . . . . .
    //2 . . . . . . . .
    //1 K . . . . . r .

    val board0 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(2, 1), Position(1, 1), King(WHITE_PLAYER, true), None)))
    executeMove(board0, BLACK_PLAYER, PlayerAction(Position(8, 8), Position(8, 1), None)) match {
      case Valid((board1, _, _, gameEvent)) =>
        gameEvent match {
          case Some(CHECK) => testMove(WHITE_PLAYER, PlayerAction(Position(3, 3), Position(2, 1), None), Knight(WHITE_PLAYER, true), board1)
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("False-Checkmate test 3") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      - Position(5, 1)
      - Position(5, 8)
      + (Position(1, 8) -> King(WHITE_PLAYER))
      + (Position(1, 1) -> King(BLACK_PLAYER))
      + (Position(2, 7) -> Pawn(WHITE_PLAYER))
      + (Position(7, 7) -> Rook(BLACK_PLAYER))
      + (Position(8, 7) -> Rook(BLACK_PLAYER))

    //a b c d e f g h
    //8 K . . . . . . .
    //7 . P . . . . r r
    //6 . . . . . . . .
    //5 . . . . . . . .
    //4 . . . . . . . .
    //3 . . . . . . . .
    //2 . . . . . . . .
    //1 k . . . . . . .

    val board0 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 7), Position(1, 8), King(WHITE_PLAYER, true), None)))
    executeMove(board0, BLACK_PLAYER, PlayerAction(Position(8, 7), Position(8, 8), None)) match {
      case Valid((board1, _, _, gameEvent)) =>
        gameEvent match {
          case Some(CHECK) => testMove(WHITE_PLAYER, PlayerAction(Position(2, 7), Position(2, 8), Some(Queen(WHITE_PLAYER, true))), Queen(WHITE_PLAYER, true), board1)
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("Actual chess game test 1") {
    // Anderssen vs. Kieseritzky (1851)
    val moves: List[(Player, PlayerAction)] = List(
      (WHITE_PLAYER, PlayerAction(Position(5, 2), Position(5, 4), None)), // 1. e4
      (BLACK_PLAYER, PlayerAction(Position(5, 7), Position(5, 5), None)), // 1... e5
      (WHITE_PLAYER, PlayerAction(Position(6, 2), Position(6, 4), None)), // 2. f4
      (BLACK_PLAYER, PlayerAction(Position(5, 5), Position(6, 4), None)), // 2... exf4
      (WHITE_PLAYER, PlayerAction(Position(6, 1), Position(3, 4), None)), // 3. Bc4
      (BLACK_PLAYER, PlayerAction(Position(4, 8), Position(8, 4), None)), // 3... Qh4+
      (WHITE_PLAYER, PlayerAction(Position(5, 1), Position(6, 1), None)), // 4. Kf1
      (BLACK_PLAYER, PlayerAction(Position(2, 7), Position(2, 5), None)), // 4... b5
      (WHITE_PLAYER, PlayerAction(Position(3, 4), Position(2, 5), None)), // 5. Bxb5
      (BLACK_PLAYER, PlayerAction(Position(7, 8), Position(6, 6), None)), // 5... Nf6
      (WHITE_PLAYER, PlayerAction(Position(7, 1), Position(6, 3), None)), // 6. Nf3
      (BLACK_PLAYER, PlayerAction(Position(8, 4), Position(8, 6), None)), // 6... Qh6
      (WHITE_PLAYER, PlayerAction(Position(4, 2), Position(4, 3), None)), // 7. d3
      (BLACK_PLAYER, PlayerAction(Position(6, 6), Position(8, 5), None)), // 7... Nh5
      (WHITE_PLAYER, PlayerAction(Position(6, 3), Position(8, 4), None)), // 8. Nh4
      (BLACK_PLAYER, PlayerAction(Position(8, 6), Position(7, 5), None)), // 8... Qg5
      (WHITE_PLAYER, PlayerAction(Position(8, 4), Position(6, 5), None)), // 9. Nf5
      (BLACK_PLAYER, PlayerAction(Position(3, 7), Position(3, 6), None)), // 9... c6
      (WHITE_PLAYER, PlayerAction(Position(7, 2), Position(7, 4), None)), // 10. g4
      (BLACK_PLAYER, PlayerAction(Position(8, 5), Position(6, 6), None)), // 10... Nf6
      (WHITE_PLAYER, PlayerAction(Position(8, 1), Position(7, 1), None)), // 11. Rg1
      (BLACK_PLAYER, PlayerAction(Position(3, 6), Position(2, 5), None)), // 11... cxb5
      (WHITE_PLAYER, PlayerAction(Position(8, 2), Position(8, 4), None)), // 12. h4
      (BLACK_PLAYER, PlayerAction(Position(7, 5), Position(7, 6), None)), // 12... Qg6
      (WHITE_PLAYER, PlayerAction(Position(8, 4), Position(8, 5), None)), // 13. h5
      (BLACK_PLAYER, PlayerAction(Position(7, 6), Position(7, 5), None)), // 13... Qg5
      (WHITE_PLAYER, PlayerAction(Position(4, 1), Position(6, 3), None)), // 14. Qf3
      (BLACK_PLAYER, PlayerAction(Position(6, 6), Position(7, 8), None)), // 14... Ng8
      (WHITE_PLAYER, PlayerAction(Position(3, 1), Position(6, 4), None)), // 15. Bxf4
      (BLACK_PLAYER, PlayerAction(Position(7, 5), Position(6, 6), None)), // 15... Qf6
      (WHITE_PLAYER, PlayerAction(Position(2, 1), Position(3, 3), None)), // 16. Nc3
      (BLACK_PLAYER, PlayerAction(Position(6, 8), Position(3, 5), None)), // 16... Bc5
      (WHITE_PLAYER, PlayerAction(Position(3, 3), Position(4, 5), None)), // 17. Nd5
      (BLACK_PLAYER, PlayerAction(Position(6, 6), Position(2, 2), None)), // 17... Qxb2
      (WHITE_PLAYER, PlayerAction(Position(6, 4), Position(4, 6), None)), // 18. Bd6
      (BLACK_PLAYER, PlayerAction(Position(3, 5), Position(7, 1), None)), // 18... Bxg1
      (WHITE_PLAYER, PlayerAction(Position(5, 4), Position(5, 5), None)), // 19. e5
      (BLACK_PLAYER, PlayerAction(Position(2, 2), Position(1, 1), None)), // 19... Qxa1+
      (WHITE_PLAYER, PlayerAction(Position(6, 1), Position(5, 2), None)), // 20. Ke2
      (BLACK_PLAYER, PlayerAction(Position(2, 8), Position(1, 6), None)), // 20... Na6
      (WHITE_PLAYER, PlayerAction(Position(6, 5), Position(7, 7), None)), // 21. Nxg7+
      (BLACK_PLAYER, PlayerAction(Position(5, 8), Position(4, 8), None)), // 21... Kd8
      (WHITE_PLAYER, PlayerAction(Position(6, 3), Position(6, 6), None)), // 22. Qf6+
      (BLACK_PLAYER, PlayerAction(Position(7, 8), Position(6, 6), None)), // 22... Nxf6
      (WHITE_PLAYER, PlayerAction(Position(4, 6), Position(5, 7), None)), // 23. Be7#
    )

    val expectedFinalPosition: Map[Position, Piece] = Map(
      Position(1,1) -> Queen(BLACK_PLAYER, true),
      Position(1,2) -> Pawn(WHITE_PLAYER, false),
      Position(1,6) -> Knight(BLACK_PLAYER, true),
      Position(1,7) -> Pawn(BLACK_PLAYER, false),
      Position(1,8) -> Rook(BLACK_PLAYER, false),
      Position(2,5) -> Pawn(BLACK_PLAYER, true),
      Position(3,2) -> Pawn(WHITE_PLAYER, false),
      Position(3,8) -> Bishop(BLACK_PLAYER, false),
      Position(4,3) -> Pawn(WHITE_PLAYER, true),
      Position(4,5) -> Knight(WHITE_PLAYER, true),
      Position(4,7) -> Pawn(BLACK_PLAYER, false),
      Position(4,8) -> King(BLACK_PLAYER, true),
      Position(5,2) -> King(WHITE_PLAYER, true),
      Position(5,5) -> Pawn(WHITE_PLAYER, true),
      Position(5,7) -> Bishop(WHITE_PLAYER, true),
      Position(6,6) -> Knight(BLACK_PLAYER, true),
      Position(6,7) -> Pawn(BLACK_PLAYER, false),
      Position(7,1) -> Bishop(BLACK_PLAYER, true),
      Position(7,4) -> Pawn(WHITE_PLAYER, true),
      Position(7,7) -> Knight(WHITE_PLAYER, true),
      Position(8,5) -> Pawn(WHITE_PLAYER, true),
      Position(8,7) -> Pawn(BLACK_PLAYER, false),
      Position(8,8) -> Rook(BLACK_PLAYER, false),
    )

    val validatedBoard = fromMovesList(moves)
    validatedBoard match
      case Valid(board) => assert(board.pieces == expectedFinalPosition)
      case Invalid(e) => fail(s"All moves should be valid: $e")
  }
}
