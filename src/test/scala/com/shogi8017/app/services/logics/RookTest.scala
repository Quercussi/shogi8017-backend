package com.shogi8017.app.services.logics

import com.shogi8017.app.exceptions.{IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.actions.{DropAction, MoveAction}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.ROOK
import com.shogi8017.app.services.logics.pieces.{Knight, PromotedRook, Rook}
import com.shogi8017.app.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class RookTest extends AnyFunSuite:
  test("A Rook should move like a cross") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Rook(WHITE_PLAYER),
        Position(3, 3) -> Rook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val testSeqBlack = (1 to 9).filter(_ != 3)
    testSeqBlack.foreach(col => testAction(BLACK_PLAYER, MoveAction(Position(3, 3), Position(3, col)), Rook(BLACK_PLAYER), s0))
    testSeqBlack.foreach(row => testAction(BLACK_PLAYER, MoveAction(Position(3, 3), Position(row, 3)), Rook(BLACK_PLAYER), s0))

    val testSeqWhite = (1 to 9).filter(_ != 4)
    testSeqWhite.foreach(col => testAction(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, col)), Rook(WHITE_PLAYER), s1))
    testSeqWhite.foreach(row => testAction(WHITE_PLAYER, MoveAction(Position(4, 4), Position(row, 4)), Rook(WHITE_PLAYER), s1))
  }

  test("A rook should not move diagonally") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(6, 6) -> Rook(WHITE_PLAYER),
        Position(4, 6) -> Rook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val testSeqBlack = (1 to 9).map(i => Position(i, 10 - i)).filterNot(_ == Position(4, 6))
    testSeqBlack.foreach(pos => testActionError(BLACK_PLAYER, MoveAction(Position(4, 6), pos), IllegalMove, s0))

    val testSeqWhite = (1 to 9).filterNot(_ == 6).map(i => Position(i, i))
    testSeqWhite.foreach(pos => testActionError(WHITE_PLAYER, MoveAction(Position(6, 6), pos), IllegalMove, s1))
  }


  test("A rook should not move like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(6, 6) -> Rook(WHITE_PLAYER),
        Position(4, 6) -> Rook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    val testSeqBlack = allPositions.filterNot(pos => pos.x == 4 || pos.y == 6)
    testSeqBlack.foreach(pos => testActionError(BLACK_PLAYER, MoveAction(Position(4, 6), pos), IllegalMove, s0))

    val testSeqWhite = allPositions.filterNot(pos => pos.x == 6 || pos.y == 6)
    testSeqWhite.foreach(pos => testActionError(WHITE_PLAYER, MoveAction(Position(6, 6), pos), IllegalMove, s1))
  }

  test("A rook should be able to jump") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(6, 6) -> Rook(WHITE_PLAYER),
        Position(4, 6) -> Rook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(BLACK_PLAYER, MoveAction(Position(4, 6), Position(8, 6)), IllegalMove, s0)
    testActionError(WHITE_PLAYER, MoveAction(Position(6, 6), Position(3, 6)), IllegalMove, s1)
  }

  test("A rook should capture like a cross") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(6, 6) -> Rook(WHITE_PLAYER),
        Position(4, 6) -> Rook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val r0 = testAction(BLACK_PLAYER, MoveAction(Position(4, 6), Position(6, 6)), Rook(BLACK_PLAYER), s0)
    assert(r0.piecesMap.size == 3)
    assert(r0.hands.get(BLACK_PLAYER).contains(Multiset(ROOK)))
    assert(r0.hands.get(WHITE_PLAYER).contains(Multiset.empty))

    val r1 = testAction(WHITE_PLAYER, MoveAction(Position(6, 6), Position(4, 6)), Rook(WHITE_PLAYER), s1)
    assert(r1.piecesMap.size == 3)
    assert(r1.hands.get(WHITE_PLAYER).contains(Multiset(ROOK)))
    assert(r1.hands.get(BLACK_PLAYER).contains(Multiset.empty))
  }

  test("A rook should not capture a piece of its own side") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(6, 6) -> Rook(WHITE_PLAYER),
        Position(6, 7) -> Knight(WHITE_PLAYER),
        Position(4, 6) -> Rook(BLACK_PLAYER),
        Position(2, 6) -> Knight(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(BLACK_PLAYER, MoveAction(Position(4, 6), Position(2, 6)), IllegalMove, s0)
    testActionError(WHITE_PLAYER, MoveAction(Position(6, 6), Position(6, 7)), IllegalMove, s1)
  }

  test("A rook should not capture like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(6, 6) -> Rook(WHITE_PLAYER),
        Position(2, 2) -> Rook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(BLACK_PLAYER, MoveAction(Position(2, 2), Position(6, 6)), IllegalMove, s0)
    testActionError(WHITE_PLAYER, MoveAction(Position(6, 6), Position(2, 2)), IllegalMove, s1)
  }

  test("Rook should be able to promote when reaching or leaving the last three ranks") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(1, 1) -> Rook(WHITE_PLAYER),
        Position(2, 8) -> Rook(WHITE_PLAYER),
        Position(3, 9) -> Rook(BLACK_PLAYER),
        Position(4, 2) -> Rook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    (1 to 3).foreach(row => testAction(BLACK_PLAYER, MoveAction(Position(3, 9), Position(3, row), false), Rook(BLACK_PLAYER), s0))
    (1 to 3).foreach(row => testAction(BLACK_PLAYER, MoveAction(Position(3, 9), Position(3, row), true),  PromotedRook(BLACK_PLAYER), s0))

    (3 to 9).foreach(row => testAction(BLACK_PLAYER, MoveAction(Position(4, 2), Position(4, row), false), Rook(BLACK_PLAYER), s0))
    (3 to 9).foreach(row => testAction(BLACK_PLAYER, MoveAction(Position(4, 2), Position(4, row), true),  PromotedRook(BLACK_PLAYER), s0))

    (7 to 9).foreach(row => testAction(WHITE_PLAYER, MoveAction(Position(1, 1), Position(1, row), false), Rook(WHITE_PLAYER), s1))
    (7 to 9).foreach(row => testAction(WHITE_PLAYER, MoveAction(Position(1, 1), Position(1, row), true),  PromotedRook(WHITE_PLAYER), s1))

    (1 to 7).foreach(row => testAction(WHITE_PLAYER, MoveAction(Position(2, 8), Position(2, row), false), Rook(WHITE_PLAYER), s1))
    (1 to 7).foreach(row => testAction(WHITE_PLAYER, MoveAction(Position(2, 8), Position(2, row), true),  PromotedRook(WHITE_PLAYER), s1))
  }

  test("Rook cannot promote outside the last three rank") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(1, 6) -> Rook(WHITE_PLAYER),
        Position(9, 4) -> Rook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    (1 to 8).foreach(col => testActionError(BLACK_PLAYER, MoveAction(Position(9, 4), Position(col, 4), true), IncorrectPromotionScenario, s0))
    (5 to 9).foreach(row => testActionError(BLACK_PLAYER, MoveAction(Position(9, 4), Position(9, row), true), IncorrectPromotionScenario, s0))
    (2 to 9).foreach(col => testActionError(WHITE_PLAYER, MoveAction(Position(1, 6), Position(col, 6), true), IncorrectPromotionScenario, s1))
    (1 to 5).foreach(row => testActionError(WHITE_PLAYER, MoveAction(Position(1, 6), Position(1, row), true), IncorrectPromotionScenario, s1))
  }

//  test("Rook must be able to return `getAllPossibleMoves` correctly") {
//    val s0 = Board.emptyBoard.copy(
//      piecesMap = Board.emptyBoard.piecesMap ++ Map(
//        Position(1, 1) -> Rook(WHITE_PLAYER),
//        Position(2, 8) -> Rook(WHITE_PLAYER),
//        Position(3, 9) -> Rook(BLACK_PLAYER),
//        Position(4, 2) -> Rook(BLACK_PLAYER)
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
//      val rook = Rook(WHITE_PLAYER)
//      val tempBoard = s0.copy(piecesMap = s0.piecesMap + (pos -> rook))
//      assert(rook.getAllPossibleMoves(tempBoard, pos) == allPositions)
//    })
//  }
  
  test("Rook should be able to drop any unoccupied position") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(ROOK),
        BLACK_PLAYER -> Multiset(ROOK)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)
  
    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)
  
    val allDroppablePosition = allPositions.filterNot(s0.piecesMap.contains)

    allDroppablePosition.foreach(pos => {
      val r0 = testAction(BLACK_PLAYER, DropAction(pos, ROOK), Rook(BLACK_PLAYER), s0)
      assert(r0.piecesMap.size == 3)
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset(ROOK)))
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })

    allDroppablePosition.foreach(pos => {
      val r1 = testAction(WHITE_PLAYER, DropAction(pos, ROOK), Rook(WHITE_PLAYER), s1)
      assert(r1.piecesMap.size == 3)
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset.empty))
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset(ROOK)))
    })
  }

  test("Rook score should be 5") {
    assert(Rook(WHITE_PLAYER).score == 5)
    assert(Rook(BLACK_PLAYER).score == 5)
  }