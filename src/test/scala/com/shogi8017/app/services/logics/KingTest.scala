package com.shogi8017.app.services.logics

import com.shogi8017.app.exceptions.{CannotPromote, IllegalMove, InvalidDropPiece}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.actions.{DropAction, MoveAction}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.KNIGHT
import com.shogi8017.app.services.logics.pieces.UnPromotablePieceType.KING
import com.shogi8017.app.services.logics.pieces.{King, Knight, Rook}
import com.shogi8017.app.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class KingTest extends AnyFunSuite:
  private def generateUStarPositions(start: Position): Seq[Position] = {
    (for (
      dx <- -1 to 1;
      dy <- -1 to 1
      if dx != 0 || dy != 0
    ) yield Direction(dx, dy))
      .map(d => start.move(d))
      .filter(!_.isOutOfBoard)
  }

  test("A King should move like a star") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(5, 2) -> King(WHITE_PLAYER),
        Position(5, 8) -> King(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val testSeqBlack = generateUStarPositions(Position(5, 8))
    testSeqBlack.foreach(pos => testAction(BLACK_PLAYER, MoveAction(Position(5, 8), pos), King(BLACK_PLAYER), s0))

    val testSeqWhite = generateUStarPositions(Position(5, 2))
    testSeqWhite.foreach(pos => testAction(WHITE_PLAYER, MoveAction(Position(5, 2), pos), King(WHITE_PLAYER), s1))
  }

  test("A King should not move like something else") {
    val s0 = Board.emptyBoard
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val blackReachablePositions = generateUStarPositions(Position(5, 9))
    val testSeqBlack = getAllPosition.filterNot(p => blackReachablePositions.contains(p) || p == Position(5, 9))
    testSeqBlack.foreach(pos => testActionError(BLACK_PLAYER, MoveAction(Position(5, 9), pos), IllegalMove, s0))

    val whiteReachablePositions = generateUStarPositions(Position(5, 1))
    val testSeqWhite = getAllPosition.filterNot(p => whiteReachablePositions.contains(p) || p == Position(5, 1))
    testSeqWhite.foreach(pos => testActionError(WHITE_PLAYER, MoveAction(Position(5, 1), pos), IllegalMove, s1))
  }

  test("A King cannot move into check") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(5, 2) -> King(WHITE_PLAYER),
        Position(5, 8) -> King(BLACK_PLAYER),

        Position(1, 1) -> Rook(BLACK_PLAYER),
        Position(1, 3) -> Rook(BLACK_PLAYER),
        Position(1, 7) -> Rook(WHITE_PLAYER),
        Position(1, 9) -> Rook(WHITE_PLAYER),
        Position(5, 6) -> Knight(WHITE_PLAYER),
        Position(5, 4) -> Knight(BLACK_PLAYER),
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    //  a b c d e f g h i
    //9 R . . . . . . . .
    //8 . . . . k . . . .
    //7 R . . . . . . . .
    //6 . . . . N . . . .
    //5 . . . . . . . . .
    //4 . . . . n . . . .
    //3 r . . . . . . . .
    //2 . . . . K . . . .
    //1 r . . . . . . . .

    val testSeqBlack = generateUStarPositions(Position(5, 8))
    testSeqBlack.foreach(pos => testActionError(BLACK_PLAYER, MoveAction(Position(5, 8), pos), IllegalMove, s0))
    val testSeqWhite = generateUStarPositions(Position(5, 2))
    testSeqWhite.foreach(pos => testActionError(WHITE_PLAYER, MoveAction(Position(5, 2), pos), IllegalMove, s1))
  }

  test("A King should capture like a star") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(5, 2) -> King(WHITE_PLAYER),
        Position(5, 8) -> King(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    generateUStarPositions(Position(5, 8)).foreach(pos =>
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      val r = testAction(BLACK_PLAYER, MoveAction(Position(5, 8), pos), King(BLACK_PLAYER), s0_temp)
      assert(r.piecesMap.size == 2)
      assert(r.hands.get(BLACK_PLAYER).contains(Multiset(KNIGHT)))
      assert(r.hands.get(WHITE_PLAYER).contains(Multiset.empty))
    )

    generateUStarPositions(Position(5, 2)).foreach(pos =>
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      val r = testAction(WHITE_PLAYER, MoveAction(Position(5, 2), pos), King(WHITE_PLAYER), s1_temp)
      assert(r.piecesMap.size == 2)
      assert(r.hands.get(WHITE_PLAYER).contains(Multiset(KNIGHT)))
      assert(r.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    )
  }

  test("A King should not capture a piece of its own side") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(5, 2) -> King(WHITE_PLAYER),
        Position(5, 8) -> King(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    generateUStarPositions(Position(5, 8)).foreach(pos =>
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(5, 8), pos), IllegalMove, s0_temp)
    )

    generateUStarPositions(Position(5, 2)).foreach(pos =>
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(5, 2), pos), IllegalMove, s1_temp)
    )
  }

  test("A King should not capture like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(5, 2) -> King(WHITE_PLAYER),
        Position(5, 8) -> King(BLACK_PLAYER),
        Position(6, 1) -> Knight(WHITE_PLAYER),
        Position(6, 4) -> Knight(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(BLACK_PLAYER, MoveAction(Position(5, 8), Position(6, 1)), IllegalMove, s0)
    testActionError(WHITE_PLAYER, MoveAction(Position(5, 2), Position(6, 4)), IllegalMove, s1)
  }

  test("King should not be able to promote") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(5, 6) -> King(WHITE_PLAYER),
        Position(5, 4) -> King(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    (4 to 6).foreach(col => testActionError(BLACK_PLAYER, MoveAction(Position(5, 4), Position(col, 3), true), CannotPromote, s0))
    (4 to 6).foreach(col => testActionError(WHITE_PLAYER, MoveAction(Position(5, 6), Position(col, 7), true), CannotPromote, s1))
  }

//  test("King must be able to return `getAllPossibleMoves` correctly") {
//    val s0 = Board.emptyBoard.copy(
//      piecesMap = Board.emptyBoard.piecesMap ++ Map(
//        Position(1, 1) -> King(WHITE_PLAYER),
//        Position(2, 8) -> King(WHITE_PLAYER),
//        Position(3, 9) -> King(BLACK_PLAYER),
//        Position(4, 2) -> King(BLACK_PLAYER)
//      )
//    )
//    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))
//    
//    val allPositions = for {
//      row <- 1 to 9
//      col <- 1 to 9
//    } yield Position(row, col)
//    
//    val allDroppablePosition = allPositions.filterNot(s0.piecesMap.contains)
//    
//    allDroppablePosition.foreach(pos => {
//      val King = King(WHITE_PLAYER)
//      val tempBoard = s0.copy(piecesMap = s0.piecesMap + (pos -> King))
//      assert(King.getAllPossibleMoves(tempBoard, pos) == allPositions)
//    })
//  }

  test("King should not be able to be dropped") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(KING),
        BLACK_PLAYER -> Multiset(KING)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    val allDroppablePosition = allPositions.filterNot(s0.piecesMap.contains)

    allDroppablePosition.foreach(pos => {
      testActionError(BLACK_PLAYER, DropAction(pos, KING), InvalidDropPiece, s0)
    })

    allDroppablePosition.foreach(pos => {
      testActionError(WHITE_PLAYER, DropAction(pos, KING), InvalidDropPiece, s1)
    })
  }
  
  test("King score should be 0") {
    assert(King(WHITE_PLAYER).score == 0)
    assert(King(BLACK_PLAYER).score == 0)
  }