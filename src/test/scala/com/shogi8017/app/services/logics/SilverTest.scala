package com.shogi8017.app.services.logics

import com.shogi8017.app.exceptions.{IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.actions.{DropAction, MoveAction}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.{KNIGHT, ROOK, SILVER}
import com.shogi8017.app.services.logics.pieces.{Knight, PromotedSilver, Silver}
import com.shogi8017.app.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class SilverTest extends AnyFunSuite:
  private def generateReachablePositions(player: Player)(start: Position): Seq[Position] = {
    val dir = if player == WHITE_PLAYER then 1 else -1
    val directions = Seq(
      Direction(-1, 1*dir), Direction(0, 1*dir), Direction(1, 1*dir),
      Direction(-1, -1*dir), Direction(1, -1*dir)
    )
    directions.map(d => start.move(d)).filter(!_.isOutOfBoard)
  }

  test("A Silver should move like a Silver") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 2) -> Silver(WHITE_PLAYER),
        Position(8, 8) -> Silver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val testSeqBlack = generateReachablePositions(BLACK_PLAYER)(Position(8, 8))
    testSeqBlack.foreach(pos => testAction(BLACK_PLAYER, MoveAction(Position(8, 8), pos), Silver(BLACK_PLAYER), s0))

    val testSeqWhite = generateReachablePositions(WHITE_PLAYER)(Position(2, 2))
    testSeqWhite.foreach(pos => testAction(WHITE_PLAYER, MoveAction(Position(2, 2), pos), Silver(WHITE_PLAYER), s1))
  }

  test("A Silver should not move like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(3, 3) -> Silver(WHITE_PLAYER),
        Position(7, 7) -> Silver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val blackReachablePositions = generateReachablePositions(BLACK_PLAYER)(Position(7, 7))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains).filterNot(_ == Position(7, 7))
    testSeqBlack.foreach(pos => testActionError(BLACK_PLAYER, MoveAction(Position(7, 7), pos), IllegalMove, s0))

    val whiteReachablePositions = generateReachablePositions(WHITE_PLAYER)(Position(3, 3))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains).filterNot(_ == Position(3, 3))
    testSeqWhite.foreach(pos => testActionError(WHITE_PLAYER, MoveAction(Position(3, 3), pos), IllegalMove, s1))
  }

  test("A Silver should capture like a Silver") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 2) -> Silver(WHITE_PLAYER),
        Position(8, 8) -> Silver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val testSeqBlack = generateReachablePositions(BLACK_PLAYER)(Position(8, 8))
    testSeqBlack.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      val r0 = testAction(BLACK_PLAYER, MoveAction(Position(8, 8), pos), Silver(BLACK_PLAYER), s0_temp)
      assert(r0.piecesMap.size == 4)
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset.empty))
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset(KNIGHT)))
    })

    val testSeqWhite = generateReachablePositions(WHITE_PLAYER)(Position(2, 2))
    testSeqWhite.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      val r1 = testAction(WHITE_PLAYER, MoveAction(Position(2, 2), pos), Silver(WHITE_PLAYER), s1_temp)
      assert(r1.piecesMap.size == 4)
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset(KNIGHT)))
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })
  }

  test("A Silver should not capture a piece of its own side") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 2) -> Silver(WHITE_PLAYER),
        Position(8, 8) -> Silver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val testSeqBlack = generateReachablePositions(BLACK_PLAYER)(Position(8, 8))
    testSeqBlack.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(8, 8), pos), IllegalMove, s0_temp)
    })

    val testSeqWhite = generateReachablePositions(WHITE_PLAYER)(Position(2, 2))
    testSeqWhite.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(2, 2), pos), IllegalMove, s1_temp)
    })
  }

  test("A Silver should not capture like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(3, 3) -> Silver(WHITE_PLAYER),
        Position(7, 7) -> Silver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val blackReachablePositions = generateReachablePositions(BLACK_PLAYER)(Position(7, 7))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains).filterNot(p => p == Position(7, 7) || p == Position(5, 1) || p == Position(5, 9))
    testSeqBlack.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(7, 7), pos), IllegalMove, s0_temp)
    })

    val whiteReachablePositions = generateReachablePositions(WHITE_PLAYER)(Position(3, 3))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains).filterNot(p => p == Position(3, 3) || p == Position(5, 1) || p == Position(5, 9))
    testSeqWhite.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(3, 3), pos), IllegalMove, s1_temp)
    })
  }

  test("Silver should be able to promote when reaching or leaving the last three ranks") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 6) -> Silver(WHITE_PLAYER),
        Position(4, 7) -> Silver(WHITE_PLAYER),
        Position(6, 4) -> Silver(BLACK_PLAYER),
        Position(8, 3) -> Silver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    (5 to 7).foreach(col => testAction(BLACK_PLAYER, MoveAction(Position(6, 4), Position(col, 3), false), Silver(BLACK_PLAYER), s0))
    (5 to 7).foreach(col => testAction(BLACK_PLAYER, MoveAction(Position(6, 4), Position(col, 3), true),  PromotedSilver(BLACK_PLAYER), s0))

    Seq(7,9).foreach(col => testAction(BLACK_PLAYER, MoveAction(Position(8, 3), Position(col, 4), false), Silver(BLACK_PLAYER), s0))
    Seq(7,9).foreach(col => testAction(BLACK_PLAYER, MoveAction(Position(8, 3), Position(col, 4), true),  PromotedSilver(BLACK_PLAYER), s0))

    (1 to 3).foreach(col => testAction(WHITE_PLAYER, MoveAction(Position(2, 6), Position(col, 7), false), Silver(WHITE_PLAYER), s1))
    (1 to 3).foreach(col => testAction(WHITE_PLAYER, MoveAction(Position(2, 6), Position(col, 7), true),  PromotedSilver(WHITE_PLAYER), s1))

    Seq(3,5).foreach(col => testAction(WHITE_PLAYER, MoveAction(Position(4, 7), Position(col, 6), false), Silver(WHITE_PLAYER), s1))
    Seq(3,5).foreach(col => testAction(WHITE_PLAYER, MoveAction(Position(4, 7), Position(col, 6), true),  PromotedSilver(WHITE_PLAYER), s1))
  }

  test("Silver cannot promote outside the last three rank") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 4) -> Silver(WHITE_PLAYER),
        Position(8, 6) -> Silver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    generateReachablePositions(BLACK_PLAYER)(Position(8, 6)).foreach(pos =>
      testActionError(BLACK_PLAYER, MoveAction(Position(8, 6), pos, true), IncorrectPromotionScenario, s0)
    )

    generateReachablePositions(WHITE_PLAYER)(Position(2, 4)).foreach(pos =>
      testActionError(WHITE_PLAYER, MoveAction(Position(2, 4), pos, true), IncorrectPromotionScenario, s1)
    )
  }

//  test("Silver must be able to return `getAllPossibleMoves` correctly") {
//    val s0 = Board.emptyBoard.copy(
//      piecesMap = Board.emptyBoard.piecesMap ++ Map(
//        Position(1, 1) -> Silver(WHITE_PLAYER),
//        Position(2, 8) -> Silver(WHITE_PLAYER),
//        Position(3, 9) -> Silver(BLACK_PLAYER),
//        Position(4, 2) -> Silver(BLACK_PLAYER)
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
//      val Silver = Silver(WHITE_PLAYER)
//      val tempBoard = s0.copy(piecesMap = s0.piecesMap + (pos -> Silver))
//      assert(Silver.getAllPossibleMoves(tempBoard, pos) == allPositions)
//    })
//  }
  
  test("Silver should be able to drop any unoccupied position") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(SILVER),
        BLACK_PLAYER -> Multiset(SILVER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    val allDroppablePosition = allPositions.filterNot(s0.piecesMap.contains)

    allDroppablePosition.foreach(pos => {
      val r0 = testAction(BLACK_PLAYER, DropAction(pos, SILVER), Silver(BLACK_PLAYER), s0)
      assert(r0.piecesMap.size == 3)
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset(SILVER)))
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })

    allDroppablePosition.foreach(pos => {
      val r1 = testAction(WHITE_PLAYER, DropAction(pos, SILVER), Silver(WHITE_PLAYER), s1)
      assert(r1.piecesMap.size == 3)
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset.empty))
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset(SILVER)))
    })
  }
  
  test("Silver score should be 1") {
    assert(Silver(WHITE_PLAYER).score == 1)
    assert(Silver(BLACK_PLAYER).score == 1)
  }