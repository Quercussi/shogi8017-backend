package com.shogi8017.app.services.logics

import cats.data.Validated.Valid
import com.shogi8017.app.errors.{IllegalMove, NotOwnerOfPiece, OutOfTurn}
import com.shogi8017.app.services.logics.Board.executeMove
import com.shogi8017.app.services.logics.GameEvent.{CHECK, CHECKMATE}
import com.shogi8017.app.services.logics.LogicTestUtils.{testAction, testActionError}
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.*
import com.shogi8017.app.services.logics.pieces.PieceType.*
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.*
import com.shogi8017.app.services.logics.pieces.UnPromotablePieceType.*
import com.shogi8017.app.services.logics.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class BoardTest extends AnyFunSuite {

  test("defaultInitialPosition should initialize the board correctly") {
    val board = Board.defaultInitialPosition

    // Check the white pieces
    val firstRankConfiguration: List[PieceType] = List(LANCE, KNIGHT, SILVER, GOLD, KING, GOLD, SILVER, KNIGHT, LANCE)
    (1 to 9).foreach(col => assert(board.piecesMap.get(Position(col, 1)).contains(getPieceByPieceType(firstRankConfiguration(col - 1), WHITE_PLAYER))))
    assert(board.piecesMap.get(Position(2, 2)).contains(Bishop(WHITE_PLAYER)))
    assert(board.piecesMap.get(Position(8, 2)).contains(Rook(WHITE_PLAYER)))
    (1 to 9).foreach(col => assert(board.piecesMap.get(Position(col, 3)).contains(Pawn(WHITE_PLAYER))))

    // Check the black pieces
    (1 to 9).foreach(col => assert(board.piecesMap.get(Position(col, 9)).contains(getPieceByPieceType(firstRankConfiguration(col - 1), BLACK_PLAYER))))
    assert(board.piecesMap.get(Position(2, 8)).contains(Rook(BLACK_PLAYER)))
    assert(board.piecesMap.get(Position(8, 8)).contains(Bishop(BLACK_PLAYER)))
    (1 to 9).foreach(col => assert(board.piecesMap.get(Position(col, 7)).contains(Pawn(BLACK_PLAYER))))
  }

  test("emptyPosition should initialize the empty board correctly") {
    val board = Board.emptyBoard

    assert(board.piecesMap == Map(
      Position(5,1) -> King(WHITE_PLAYER),
      Position(5,9) -> King(BLACK_PLAYER)
    ))
  }

  test("A player should not be able to control opponent's pieces") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(5, 3) -> Rook(WHITE_PLAYER),
        Position(1, 8) -> Rook(BLACK_PLAYER),
      ),
    )

    testActionError(WHITE_PLAYER, MoveAction(Position(1, 8), Position(2, 8)), NotOwnerOfPiece, s0)
  }

  test("A player should not be able to move when it's not their turn") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(1, 3) -> Rook(WHITE_PLAYER),
        Position(1, 8) -> Rook(BLACK_PLAYER),
      ),
      hands = Map(WHITE_PLAYER -> Multiset(PAWN), BLACK_PLAYER -> Multiset(PAWN)),
      lastAction = Some(Action(WHITE_PLAYER))
    )
    val s1 = s0.copy(lastAction = Some(Action(BLACK_PLAYER)))

    testActionError(WHITE_PLAYER, MoveAction(Position(1, 3), Position(1, 7)), OutOfTurn, s0)
    testActionError(WHITE_PLAYER, DropAction(Position(1, 7), PAWN), OutOfTurn, s0)
    testActionError(BLACK_PLAYER, MoveAction(Position(1, 8), Position(8, 8)), OutOfTurn, s1)
    testActionError(BLACK_PLAYER, DropAction(Position(8, 8), PAWN), OutOfTurn, s1)
  }

  //  test("board should recognize a stalemate") {
//    val board = Board.emptyBoard
//    val newPieces = board.piecesMap
//      - Position(5,1)
//      + (Position(1,2) -> King(WHITE_PLAYER))
//      + (Position(2,8) -> Rook(BLACK_PLAYER))
//      + (Position(8,8) -> Rook(BLACK_PLAYER))
//
//    val board0 = Board(newPieces)
//    val board1 = testMove(WHITE_PLAYER, MoveAction(Position(1, 2), Position(1, 1), None), King(WHITE_PLAYER, true), board0)
//
//    executeMove(board1, BLACK_PLAYER, MoveAction(Position(8, 8), Position(8, 2), None)) match {
//      case Valid((_, _, _, gameEvent)) =>
//        gameEvent match {
//          case Some(STALEMATE) => ()
//          case None => fail("There should be a game event")
//          case Some(e) => fail(s"Incorrect game event: $e")
//        }
//      case _ => fail("Move should be valid")
//    }
//  }
//
//  test("board should recognize a dead position (king-vs-king)") {
//    val board = Board.emptyBoard
//    val newPieces = board.piecesMap
//      + (Position(4,1) -> Queen(BLACK_PLAYER))
//
//    val board0 = Board(newPieces)
//    val board1 = testMove(WHITE_PLAYER, MoveAction(Position(5, 1), Position(4, 1), None), King(WHITE_PLAYER, true), board0)
//
//    executeMove(board1, BLACK_PLAYER, MoveAction(Position(5, 8), Position(4, 8), None)) match {
//      case Valid((_, _, _, gameEvent)) =>
//        gameEvent match {
//          case Some(DEAD_POSITION) => ()
//          case None => fail("There should be a game event")
//          case Some(e) => fail(s"Incorrect game event: $e")
//        }
//      case _ => fail("Move should be valid")
//    }
//  }
//
//  test("board should recognize a dead position (king-vs-king-n-bishop)") {
//    val board = Board.emptyBoard
//    val newPieces = board.piecesMap
//      + (Position(4, 2) -> Bishop(BLACK_PLAYER))
//      + (Position(4, 1) -> Queen(BLACK_PLAYER))
//
//    val board0 = Board(newPieces)
//    val board1 = testMove(WHITE_PLAYER, MoveAction(Position(5, 1), Position(4, 1), None), King(WHITE_PLAYER, true), board0)
//
//    executeMove(board1, BLACK_PLAYER, MoveAction(Position(5, 8), Position(4, 8), None)) match {
//      case Valid((_, _, _, gameEvent)) =>
//        gameEvent match {
//          case Some(DEAD_POSITION) => ()
//          case None => fail("There should be a game event")
//          case Some(e) => fail(s"Incorrect game event: $e")
//        }
//      case _ => fail("Move should be valid")
//    }
//  }
//
//  test("board should recognize a dead position (king-n-bishop-vs-king-n-bishop)") {
//    val board = Board.emptyBoard
//    val newPieces = board.piecesMap
//      + (Position(4, 2) -> Bishop(BLACK_PLAYER))
//      + (Position(4, 1) -> Queen(BLACK_PLAYER))
//      + (Position(1, 1) -> Bishop(WHITE_PLAYER))
//
//    val board0 = Board(newPieces)
//    val board1 = testMove(WHITE_PLAYER, MoveAction(Position(5, 1), Position(4, 1), None), King(WHITE_PLAYER, true), board0)
//
//    executeMove(board1, BLACK_PLAYER, MoveAction(Position(5, 8), Position(4, 8), None)) match {
//      case Valid((_, _, _, gameEvent)) =>
//        gameEvent match {
//          case Some(DEAD_POSITION) => ()
//          case None => fail("There should be a game event")
//          case Some(e) => fail(s"Incorrect game event: $e")
//        }
//      case _ => fail("Move should be valid")
//    }
//  }

  test("Board should recognize a checkmate") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(1,1) -> King(WHITE_PLAYER),
        Position(3,4) -> Pawn(WHITE_PLAYER),
        Position(5,9) -> King(BLACK_PLAYER),
        Position(2,8) -> Rook(BLACK_PLAYER),
        Position(7,2) -> Rook(BLACK_PLAYER),
        Position(8,8) -> Rook(BLACK_PLAYER)
      ),
      lastAction = Some(Action(WHITE_PLAYER))
    )

    //  a b c d e f g h i
    //9 . . . . K . . . .
    //8 . r . . . . . r .
    //7 . . . . . . . . .
    //6 . . . . . . . . .
    //5 . . . . . . . . .
    //4 . . P . . . . . .
    //3 . . . . . . . . .
    //2 . . . . . . r . .
    //1 K . . . . . . . .

    executeMove(s0, BLACK_PLAYER, MoveAction(Position(8, 8), Position(8, 1))) match {
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
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(5,3) -> Rook(WHITE_PLAYER),
        Position(5,8) -> Rook(BLACK_PLAYER),
      ),
    )

    testActionError(WHITE_PLAYER, MoveAction(Position(5, 3), Position(4, 3)), IllegalMove, s0)
  }

  test("False-Checkmate test 1") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(1,1) -> King(WHITE_PLAYER),
        Position(5,9) -> King(BLACK_PLAYER),
        Position(2,7) -> Rook(WHITE_PLAYER),
        Position(7,2) -> Rook(BLACK_PLAYER),
        Position(8,8) -> Rook(BLACK_PLAYER),
      ),
      lastAction = Some(Action(WHITE_PLAYER))
    )

    //  a b c d e f g h i
    //9 . . . . K . . . .
    //8 . . . . . . . r .
    //7 . R . . . . . . .
    //6 . . . . . . . . .
    //5 . . . . . . . . .
    //4 . . P . . . . . .
    //3 . . . . . . . . .
    //2 . . . . . . r . .
    //1 K . . . . . . . .

    executeMove(s0, BLACK_PLAYER, MoveAction(Position(8, 8), Position(8, 1))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent match {
          case Some(CHECK) => testAction(WHITE_PLAYER, MoveAction(Position(2, 7), Position(2, 1)), Rook(WHITE_PLAYER), s1)
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("False-Checkmate test 2") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(1,1) -> King(WHITE_PLAYER),
        Position(5,9) -> King(BLACK_PLAYER),
        Position(9,2) -> Bishop(WHITE_PLAYER),
        Position(7,2) -> Rook(BLACK_PLAYER),
        Position(8,6) -> Rook(BLACK_PLAYER),
      ),
      lastAction = Some(Action(WHITE_PLAYER))
    )

    //  a b c d e f g h i
    //9 . . . . K . . . .
    //8 . . . . . . . . .
    //7 . . . . . . . . .
    //6 . . . . . . . r .
    //5 . . . . . . . . .
    //4 . . P . . . . . .
    //3 . . . . . . . . .
    //2 . . . . . . r . B
    //1 K . . . . . . . .

    executeMove(s0, BLACK_PLAYER, MoveAction(Position(8, 6), Position(8, 1))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent match {
          case Some(CHECK) => testAction(WHITE_PLAYER, MoveAction(Position(9, 2), Position(8, 1)), Bishop(WHITE_PLAYER), s1)
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("False-Checkmate test 3") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(1,1) -> King(WHITE_PLAYER),
        Position(5,9) -> King(BLACK_PLAYER),
        Position(7,2) -> Rook(BLACK_PLAYER),
        Position(8,6) -> Rook(BLACK_PLAYER),
      ),
      hands = Map(WHITE_PLAYER -> Multiset(KNIGHT), BLACK_PLAYER -> Multiset.empty),
      lastAction = Some(Action(WHITE_PLAYER))
    )

    //  a b c d e f g h i
    //9 . . . . K . . . .
    //8 . . . . . . . . .
    //7 . . . . . . . . .
    //6 . . . . . . . r .
    //5 . . . . . . . . .
    //4 . . P . . . . . .
    //3 . . . . . . . . .
    //2 . . . . . . r . .
    //1 K . . . . . . . .

    executeMove(s0, BLACK_PLAYER, MoveAction(Position(8, 6), Position(8, 1))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent match {
          case Some(CHECK) => testAction(WHITE_PLAYER, DropAction(Position(2, 1), KNIGHT), Knight(WHITE_PLAYER), s1)
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

//
//  test("Actual chess game test 1") {
//    // Anderssen vs. Kieseritzky (1851)
//    val moves: List[(Player, MoveAction)] = List(
//      (WHITE_PLAYER, MoveAction(Position(5, 2), Position(5, 4), None)), // 1. e4
//      (BLACK_PLAYER, MoveAction(Position(5, 7), Position(5, 5), None)), // 1... e5
//      (WHITE_PLAYER, MoveAction(Position(6, 2), Position(6, 4), None)), // 2. f4
//      (BLACK_PLAYER, MoveAction(Position(5, 5), Position(6, 4), None)), // 2... exf4
//      (WHITE_PLAYER, MoveAction(Position(6, 1), Position(3, 4), None)), // 3. Bc4
//      (BLACK_PLAYER, MoveAction(Position(4, 8), Position(8, 4), None)), // 3... Qh4+
//      (WHITE_PLAYER, MoveAction(Position(5, 1), Position(6, 1), None)), // 4. Kf1
//      (BLACK_PLAYER, MoveAction(Position(2, 7), Position(2, 5), None)), // 4... b5
//      (WHITE_PLAYER, MoveAction(Position(3, 4), Position(2, 5), None)), // 5. Bxb5
//      (BLACK_PLAYER, MoveAction(Position(7, 8), Position(6, 6), None)), // 5... Nf6
//      (WHITE_PLAYER, MoveAction(Position(7, 1), Position(6, 3), None)), // 6. Nf3
//      (BLACK_PLAYER, MoveAction(Position(8, 4), Position(8, 6), None)), // 6... Qh6
//      (WHITE_PLAYER, MoveAction(Position(4, 2), Position(4, 3), None)), // 7. d3
//      (BLACK_PLAYER, MoveAction(Position(6, 6), Position(8, 5), None)), // 7... Nh5
//      (WHITE_PLAYER, MoveAction(Position(6, 3), Position(8, 4), None)), // 8. Nh4
//      (BLACK_PLAYER, MoveAction(Position(8, 6), Position(7, 5), None)), // 8... Qg5
//      (WHITE_PLAYER, MoveAction(Position(8, 4), Position(6, 5), None)), // 9. Nf5
//      (BLACK_PLAYER, MoveAction(Position(3, 7), Position(3, 6), None)), // 9... c6
//      (WHITE_PLAYER, MoveAction(Position(7, 2), Position(7, 4), None)), // 10. g4
//      (BLACK_PLAYER, MoveAction(Position(8, 5), Position(6, 6), None)), // 10... Nf6
//      (WHITE_PLAYER, MoveAction(Position(8, 1), Position(7, 1), None)), // 11. Rg1
//      (BLACK_PLAYER, MoveAction(Position(3, 6), Position(2, 5), None)), // 11... cxb5
//      (WHITE_PLAYER, MoveAction(Position(8, 2), Position(8, 4), None)), // 12. h4
//      (BLACK_PLAYER, MoveAction(Position(7, 5), Position(7, 6), None)), // 12... Qg6
//      (WHITE_PLAYER, MoveAction(Position(8, 4), Position(8, 5), None)), // 13. h5
//      (BLACK_PLAYER, MoveAction(Position(7, 6), Position(7, 5), None)), // 13... Qg5
//      (WHITE_PLAYER, MoveAction(Position(4, 1), Position(6, 3), None)), // 14. Qf3
//      (BLACK_PLAYER, MoveAction(Position(6, 6), Position(7, 8), None)), // 14... Ng8
//      (WHITE_PLAYER, MoveAction(Position(3, 1), Position(6, 4), None)), // 15. Bxf4
//      (BLACK_PLAYER, MoveAction(Position(7, 5), Position(6, 6), None)), // 15... Qf6
//      (WHITE_PLAYER, MoveAction(Position(2, 1), Position(3, 3), None)), // 16. Nc3
//      (BLACK_PLAYER, MoveAction(Position(6, 8), Position(3, 5), None)), // 16... Bc5
//      (WHITE_PLAYER, MoveAction(Position(3, 3), Position(4, 5), None)), // 17. Nd5
//      (BLACK_PLAYER, MoveAction(Position(6, 6), Position(2, 2), None)), // 17... Qxb2
//      (WHITE_PLAYER, MoveAction(Position(6, 4), Position(4, 6), None)), // 18. Bd6
//      (BLACK_PLAYER, MoveAction(Position(3, 5), Position(7, 1), None)), // 18... Bxg1
//      (WHITE_PLAYER, MoveAction(Position(5, 4), Position(5, 5), None)), // 19. e5
//      (BLACK_PLAYER, MoveAction(Position(2, 2), Position(1, 1), None)), // 19... Qxa1+
//      (WHITE_PLAYER, MoveAction(Position(6, 1), Position(5, 2), None)), // 20. Ke2
//      (BLACK_PLAYER, MoveAction(Position(2, 8), Position(1, 6), None)), // 20... Na6
//      (WHITE_PLAYER, MoveAction(Position(6, 5), Position(7, 7), None)), // 21. Nxg7+
//      (BLACK_PLAYER, MoveAction(Position(5, 8), Position(4, 8), None)), // 21... Kd8
//      (WHITE_PLAYER, MoveAction(Position(6, 3), Position(6, 6), None)), // 22. Qf6+
//      (BLACK_PLAYER, MoveAction(Position(7, 8), Position(6, 6), None)), // 22... Nxf6
//      (WHITE_PLAYER, MoveAction(Position(4, 6), Position(5, 7), None)), // 23. Be7#
//    )
//
//    val expectedFinalPosition: Map[Position, Piece] = Map(
//      Position(1,1) -> Queen(BLACK_PLAYER, true),
//      Position(1,2) -> Pawn(WHITE_PLAYER, false),
//      Position(1,6) -> Knight(BLACK_PLAYER, true),
//      Position(1,7) -> Pawn(BLACK_PLAYER, false),
//      Position(1,8) -> Rook(BLACK_PLAYER, false),
//      Position(2,5) -> Pawn(BLACK_PLAYER, true),
//      Position(3,2) -> Pawn(WHITE_PLAYER, false),
//      Position(3,8) -> Bishop(BLACK_PLAYER, false),
//      Position(4,3) -> Pawn(WHITE_PLAYER, true),
//      Position(4,5) -> Knight(WHITE_PLAYER, true),
//      Position(4,7) -> Pawn(BLACK_PLAYER, false),
//      Position(4,8) -> King(BLACK_PLAYER, true),
//      Position(5,2) -> King(WHITE_PLAYER, true),
//      Position(5,5) -> Pawn(WHITE_PLAYER, true),
//      Position(5,7) -> Bishop(WHITE_PLAYER, true),
//      Position(6,6) -> Knight(BLACK_PLAYER, true),
//      Position(6,7) -> Pawn(BLACK_PLAYER, false),
//      Position(7,1) -> Bishop(BLACK_PLAYER, true),
//      Position(7,4) -> Pawn(WHITE_PLAYER, true),
//      Position(7,7) -> Knight(WHITE_PLAYER, true),
//      Position(8,5) -> Pawn(WHITE_PLAYER, true),
//      Position(8,7) -> Pawn(BLACK_PLAYER, false),
//      Position(8,8) -> Rook(BLACK_PLAYER, false),
//    )
//
//    val validatedBoard = fromMovesList(moves)
//    validatedBoard match
//      case Valid(board) => assert(board.piecesMap == expectedFinalPosition)
//      case Invalid(e) => fail(s"All moves should be valid: $e")
//  }
}
