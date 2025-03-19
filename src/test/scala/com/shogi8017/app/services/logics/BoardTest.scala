package com.shogi8017.app.services.logics

import cats.data.Validated.{Invalid, Valid}
import com.shogi8017.app.exceptions.{IllegalMove, NotOwnerOfPiece, OutOfTurn}
import com.shogi8017.app.models.enumerators.GameWinner.{BLACK_WINNER, DRAW, WHITE_WINNER}
import com.shogi8017.app.services.logics.Board.{executeOnBoardAction, executionAction}
import com.shogi8017.app.services.logics.GameEvent.{CHECK, CHECKMATE}
import com.shogi8017.app.services.logics.LogicTestUtils.{fromMovesList, testAction, testActionError}
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.actions.{DropAction, ExecutionAction, MoveAction}
import com.shogi8017.app.services.logics.pieces.*
import com.shogi8017.app.services.logics.pieces.PieceType.*
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.*
import com.shogi8017.app.services.logics.pieces.UnPromotablePieceType.*
import com.shogi8017.app.utils.Multiset
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
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(WHITE_PLAYER, MoveAction(Position(1, 8), Position(2, 8)), NotOwnerOfPiece, s1)
  }

  test("A player should not be able to move when it's not their turn") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(1, 3) -> Rook(WHITE_PLAYER),
        Position(1, 8) -> Rook(BLACK_PLAYER),
      ),
      hands = Map(WHITE_PLAYER -> Multiset(PAWN), BLACK_PLAYER -> Multiset(PAWN)),
      currentPlayerTurn = BLACK_PLAYER
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(WHITE_PLAYER, MoveAction(Position(1, 3), Position(1, 7)), OutOfTurn, s0)
    testActionError(WHITE_PLAYER, DropAction(Position(1, 7), PAWN), OutOfTurn, s0)
    testActionError(BLACK_PLAYER, MoveAction(Position(1, 8), Position(8, 8)), OutOfTurn, s1)
    testActionError(BLACK_PLAYER, DropAction(Position(8, 8), PAWN), OutOfTurn, s1)
  }

  test("Black player should start first") {
    val s0 = Board.defaultInitialPosition

    testAction(BLACK_PLAYER, MoveAction(Position(3, 7), Position(3, 6)), Pawn(BLACK_PLAYER), s0)
    testActionError(WHITE_PLAYER, MoveAction(Position(3, 3), Position(3, 4)), OutOfTurn, s0)
  }

  test("board should recognize a stalemate") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(1,2) -> King(WHITE_PLAYER),
        Position(5,9) -> King(BLACK_PLAYER),
        Position(2,8) -> Rook(BLACK_PLAYER),
        Position(8,8) -> Rook(BLACK_PLAYER)
      ),
      currentPlayerTurn = WHITE_PLAYER
    )

    //  a b c d e f g h i
    //9 . . . . k . . . .
    //8 . r . . . . . r .
    //7 . . . . . . . . .
    //6 . . . . . . . . .
    //5 . . . . . . . . .
    //4 . . . . . . . . .
    //3 . . . . . . . . .
    //2 K . . . . . . . .
    //1 . . . . . . . . .

    val s1 = testAction(WHITE_PLAYER, MoveAction(Position(1, 2), Position(1, 1)), King(WHITE_PLAYER), s0)

    executionAction(s1, BLACK_PLAYER, MoveAction(Position(8, 8), Position(8, 2))) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent.gameEvent match {
          case Some(GameEvent.STALEMATE) => ()
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("board should recognize a false stalemate") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(1, 2) -> King(WHITE_PLAYER),
        Position(5, 9) -> King(BLACK_PLAYER),
        Position(2, 8) -> Rook(BLACK_PLAYER),
        Position(8, 8) -> Rook(BLACK_PLAYER)
      ),
      hands = Map(WHITE_PLAYER -> Multiset(KNIGHT), BLACK_PLAYER -> Multiset.empty),
      currentPlayerTurn = WHITE_PLAYER
    )

    //  a b c d e f g h i
    //9 . . . . k . . . .
    //8 . r . . . . . r .
    //7 . . . . . . . . .
    //6 . . . . . . . . .
    //5 . . . . . . . . .
    //4 . . . . . . . . .
    //3 . . . . . . . . .
    //2 K . . . . . . . .
    //1 . . . . . . . . .

    val s1 = testAction(WHITE_PLAYER, MoveAction(Position(1, 2), Position(1, 1)), King(WHITE_PLAYER), s0)

    executionAction(s1, BLACK_PLAYER, MoveAction(Position(8, 8), Position(8, 2))) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent.gameEvent match {
          case None => ()
          case _ => fail("There should not be a game event")
        }
      case _ => fail("Move should be valid")
    }
  }

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
      currentPlayerTurn = BLACK_PLAYER
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

    executeOnBoardAction(s0, BLACK_PLAYER, MoveAction(Position(8, 8), Position(8, 1))) match {
      case Valid((_, _, _, gameEvent)) =>
        gameEvent.gameEvent match {
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
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(WHITE_PLAYER, MoveAction(Position(5, 3), Position(4, 3)), IllegalMove, s1)
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
      currentPlayerTurn = BLACK_PLAYER
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

    executeOnBoardAction(s0, BLACK_PLAYER, MoveAction(Position(8, 8), Position(8, 1))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent.gameEvent match {
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
      currentPlayerTurn = BLACK_PLAYER
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

    executeOnBoardAction(s0, BLACK_PLAYER, MoveAction(Position(8, 6), Position(8, 1))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent.gameEvent match {
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
      currentPlayerTurn = BLACK_PLAYER
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

    executeOnBoardAction(s0, BLACK_PLAYER, MoveAction(Position(8, 6), Position(8, 1))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent.gameEvent match {
          case Some(CHECK) => testAction(WHITE_PLAYER, DropAction(Position(2, 1), KNIGHT), Knight(WHITE_PLAYER), s1)
          case None => fail("There should be a game event")
          case Some(e) => fail(s"Incorrect game event: $e")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("A board can check a draw by impasse") {
    val handMap = Map(
      KNIGHT -> 2,
      LANCE -> 2,
      SILVER -> 2,
      GOLD -> 2,
      BISHOP -> 1,
      ROOK -> 1,
      PAWN -> 9
    )

    val s0 = Board(
      piecesMap = Map(
        Position(5,7) -> King(WHITE_PLAYER),
        Position(5,4) -> King(BLACK_PLAYER),
      ),
      hands = Map(WHITE_PLAYER -> Multiset(handMap), BLACK_PLAYER -> Multiset(handMap)),
      auxiliaryState = BoardAuxiliaryState(gameWinner = None),
      currentPlayerTurn = BLACK_PLAYER
    )

    executeOnBoardAction(s0, BLACK_PLAYER, MoveAction(Position(5, 4), Position(5, 3))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent.winner match {
          case Some(DRAW) => ()
          case Some(w) => fail(s"It should have been a draw. Winner: $w")
          case None => fail("There should be a game event")
        }
      case _ => fail("Move should be valid")
    }
  }

  test("A board can check a winner by impasse 1") {
    val winnerHandMap = Map(
      KNIGHT -> 2,
      LANCE -> 2,
      SILVER -> 2,
      GOLD -> 2,
      BISHOP -> 1,
      ROOK -> 1,
      PAWN -> 9
    )

    val loserHandMap = Map(
      KNIGHT -> 2,
      LANCE -> 2,
      SILVER -> 2,
      GOLD -> 2,
      PAWN -> 9
    )

    val s0 = Board(
      piecesMap = Map(
        Position(5,7) -> King(WHITE_PLAYER),
        Position(5,4) -> King(BLACK_PLAYER),
        Position(4,4) -> Rook(BLACK_PLAYER),
        Position(6,4) -> Bishop(BLACK_PLAYER),
      ),
      hands = Map(WHITE_PLAYER -> Multiset(loserHandMap), BLACK_PLAYER -> Multiset(winnerHandMap)),
      auxiliaryState = BoardAuxiliaryState(gameWinner = None),
      currentPlayerTurn = BLACK_PLAYER
    )

    executeOnBoardAction(s0, BLACK_PLAYER, MoveAction(Position(5, 4), Position(5, 3))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent.winner match {
          case Some(BLACK_WINNER) => ()
          case Some(w) => fail(s"Black should have won. Winner: $w")
          case None => fail("There should be a game event")
        }
      case _ => fail("Move should be valid")
    }


  }

  test("A board can check a winner by impasse 2") {
    val winnerHandMap = Map(
      KNIGHT -> 2,
      LANCE -> 2,
      SILVER -> 2,
      GOLD -> 2,
      BISHOP -> 1,
      ROOK -> 1,
      PAWN -> 9
    )

    val loserHandMap = Map(
      KNIGHT -> 2,
      LANCE -> 2,
      SILVER -> 2,
      GOLD -> 2,
      PAWN -> 9
    )

    val s0 = Board(
      piecesMap = Map(
        Position(5,6) -> King(WHITE_PLAYER),
        Position(5,3) -> King(BLACK_PLAYER),
        Position(4,4) -> Rook(WHITE_PLAYER),
        Position(2,4) -> Bishop(WHITE_PLAYER),
      ),
      hands = Map(WHITE_PLAYER -> Multiset(winnerHandMap), BLACK_PLAYER -> Multiset(loserHandMap)),
      auxiliaryState = BoardAuxiliaryState(gameWinner = None),
      currentPlayerTurn = WHITE_PLAYER
    )

    executeOnBoardAction(s0, WHITE_PLAYER, MoveAction(Position(5, 6), Position(5, 7))) match {
      case Valid((s1, _, _, gameEvent)) =>
        gameEvent.winner match {
          case Some(WHITE_WINNER) => ()
          case Some(w) => fail(s"White should have won. Winner: $w")
          case None => fail("There should be a game event")
        }
      case _ => fail("Move should be valid")
    }
  }


    test("Actual chess game test 1") {
      // Shortest game possible
      val moves: List[(Player, MoveAction)] = List(
        (BLACK_PLAYER, MoveAction(Position(7, 7), Position(7, 6), false)),
        (WHITE_PLAYER, MoveAction(Position(6, 1), Position(7, 2), false)),
        (BLACK_PLAYER, MoveAction(Position(8, 8), Position(3, 3), true)),
      )

//      val expectedFinalPosition: Map[Position, Piece] = Map(
//        Position(1,1) -> Queen(BLACK_PLAYER, true),
//        Position(1,2) -> Pawn(WHITE_PLAYER, false),
//        Position(1,6) -> Knight(BLACK_PLAYER, true),
//        Position(1,7) -> Pawn(BLACK_PLAYER, false),
//        Position(1,8) -> Rook(BLACK_PLAYER, false),
//        Position(2,5) -> Pawn(BLACK_PLAYER, true),
//        Position(3,2) -> Pawn(WHITE_PLAYER, false),
//        Position(3,8) -> Bishop(BLACK_PLAYER, false),
//        Position(4,3) -> Pawn(WHITE_PLAYER, true),
//        Position(4,5) -> Knight(WHITE_PLAYER, true),
//        Position(4,7) -> Pawn(BLACK_PLAYER, false),
//        Position(4,8) -> King(BLACK_PLAYER, true),
//        Position(5,2) -> King(WHITE_PLAYER, true),
//        Position(5,5) -> Pawn(WHITE_PLAYER, true),
//        Position(5,7) -> Bishop(WHITE_PLAYER, true),
//        Position(6,6) -> Knight(BLACK_PLAYER, true),
//        Position(6,7) -> Pawn(BLACK_PLAYER, false),
//        Position(7,1) -> Bishop(BLACK_PLAYER, true),
//        Position(7,4) -> Pawn(WHITE_PLAYER, true),
//        Position(7,7) -> Knight(WHITE_PLAYER, true),
//        Position(8,5) -> Pawn(WHITE_PLAYER, true),
//        Position(8,7) -> Pawn(BLACK_PLAYER, false),
//        Position(8,8) -> Rook(BLACK_PLAYER, false),
//      )

      val validatedBoard = fromMovesList(moves.map((player, action) => ExecutionAction(player, action)))
      validatedBoard match
        case Valid(board) => assert(true)
        case Invalid(e) => fail(s"All moves should be valid: $e")
    }
}
