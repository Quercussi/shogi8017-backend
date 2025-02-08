package com.shogi8017.app.services.logics

import com.shogi8017.app.exceptions.{CannotPromote, IllegalMove, InvalidDropPiece}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.{PAWN, SILVER}
import com.shogi8017.app.services.logics.pieces.PromotedPieceType.P_SILVER
import com.shogi8017.app.services.logics.pieces.{Knight, PromotedPawn, PromotedSilver}
import com.shogi8017.app.services.logics.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class PromotedSilverTest extends AnyFunSuite:
  private def generateReachablePositions(player: Player)(start: Position): Seq[Position] = {
    val dir = if player == WHITE_PLAYER then 1 else -1
    val directions = Seq(
      Direction(-1, 1*dir), Direction(0, 1*dir), Direction(1, 1*dir),
      Direction(-1, 0), Direction(1, 0),
      Direction(0, -1*dir),
    )
    directions.map(d => start.move(d)).filter(!_.isOutOfBoard)
  }

  test("A PromotedSilver should move like a PromotedSilver") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 2) -> PromotedSilver(WHITE_PLAYER),
        Position(8, 8) -> PromotedSilver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite = generateReachablePositions(WHITE_PLAYER)(Position(2, 2))
    testSeqWhite.foreach(pos => testAction(WHITE_PLAYER, MoveAction(Position(2, 2), pos), PromotedSilver(WHITE_PLAYER), s0))

    val testSeqBlack = generateReachablePositions(BLACK_PLAYER)(Position(8, 8))
    testSeqBlack.foreach(pos => testAction(BLACK_PLAYER, MoveAction(Position(8, 8), pos), PromotedSilver(BLACK_PLAYER), s1))
  }

  test("A PromotedSilver should not move like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(3, 3) -> PromotedSilver(WHITE_PLAYER),
        Position(7, 7) -> PromotedSilver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val whiteReachablePositions = generateReachablePositions(WHITE_PLAYER)(Position(3, 3))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains).filterNot(_ == Position(3, 3))
    testSeqWhite.foreach(pos => testActionError(WHITE_PLAYER, MoveAction(Position(3, 3), pos), IllegalMove, s0))

    val blackReachablePositions = generateReachablePositions(BLACK_PLAYER)(Position(7, 7))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains).filterNot(_ == Position(7, 7))
    testSeqBlack.foreach(pos => testActionError(BLACK_PLAYER, MoveAction(Position(7, 7), pos), IllegalMove, s1))
  }

  test("A PromotedSilver should capture like a PromotedSilver") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 2) -> PromotedSilver(WHITE_PLAYER),
        Position(8, 8) -> PromotedSilver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite = generateReachablePositions(WHITE_PLAYER)(Position(2, 2))
    testSeqWhite.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> PromotedSilver(BLACK_PLAYER)))
      val r0 = testAction(WHITE_PLAYER, MoveAction(Position(2, 2), pos), PromotedSilver(WHITE_PLAYER), s0_temp)
      assert(r0.piecesMap.size == 4)
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset(SILVER)))
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })

    val testSeqBlack = generateReachablePositions(BLACK_PLAYER)(Position(8, 8))
    testSeqBlack.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> PromotedPawn(WHITE_PLAYER)))
      val r1 = testAction(BLACK_PLAYER, MoveAction(Position(8, 8), pos), PromotedSilver(BLACK_PLAYER), s1_temp)
      assert(r1.piecesMap.size == 4)
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset(PAWN)))
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset.empty))
    })
  }

  test("A PromotedSilver should not capture a piece of its own side") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 2) -> PromotedSilver(WHITE_PLAYER),
        Position(8, 8) -> PromotedSilver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite = generateReachablePositions(WHITE_PLAYER)(Position(2, 2))
    testSeqWhite.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(2, 2), pos), IllegalMove, s0_temp)
    })

    val testSeqBlack = generateReachablePositions(BLACK_PLAYER)(Position(8, 8))
    testSeqBlack.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(8, 8), pos), IllegalMove, s1_temp)
    })
  }

  test("A PromotedSilver should not capture like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(3, 3) -> PromotedSilver(WHITE_PLAYER),
        Position(7, 7) -> PromotedSilver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val whiteReachablePositions = generateReachablePositions(WHITE_PLAYER)(Position(3, 3))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains).filterNot(p => p == Position(3, 3) || p == Position(5, 1) || p == Position(5, 9))
    testSeqWhite.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(3, 3), pos), IllegalMove, s0_temp)
    })

    val blackReachablePositions = generateReachablePositions(BLACK_PLAYER)(Position(7, 7))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains).filterNot(p => p == Position(7, 7) || p == Position(5, 1) || p == Position(5, 9))
    testSeqBlack.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(7, 7), pos), IllegalMove, s1_temp)
    })
  }

  test("PromotedSilver should not be able to promote") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 6) -> PromotedSilver(WHITE_PLAYER),
        Position(8, 4) -> PromotedSilver(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    generateReachablePositions(WHITE_PLAYER)(Position(2, 6)).filter(_.y >= 7).foreach(pos =>
      testActionError(WHITE_PLAYER, MoveAction(Position(2, 6), pos, true), CannotPromote, s0)
    )

    generateReachablePositions(BLACK_PLAYER)(Position(8, 4)).filter(_.y <= 3).foreach(pos =>
      testActionError(BLACK_PLAYER, MoveAction(Position(8, 4), pos, true), CannotPromote, s1)
    )
  }

//  test("PromotedSilver must be able to return `getAllPossibleMoves` correctly") {
//    val s0 = Board.emptyBoard.copy(
//      piecesMap = Board.emptyBoard.piecesMap ++ Map(
//        Position(1, 1) -> PromotedSilver(WHITE_PLAYER),
//        Position(2, 8) -> PromotedSilver(WHITE_PLAYER),
//        Position(3, 9) -> PromotedSilver(BLACK_PLAYER),
//        Position(4, 2) -> PromotedSilver(BLACK_PLAYER)
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
//      val PromotedSilver = PromotedSilver(WHITE_PLAYER)
//      val tempBoard = s0.copy(piecesMap = s0.piecesMap + (pos -> PromotedSilver))
//      assert(PromotedSilver.getAllPossibleMoves(tempBoard, pos) == allPositions)
//    })
//  }

  test("Promoted Silver should not be able to be dropped") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(P_SILVER),
        BLACK_PLAYER -> Multiset(P_SILVER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    val allDroppablePosition = allPositions.filterNot(s0.piecesMap.contains)

    allDroppablePosition.foreach(pos => {
      testActionError(WHITE_PLAYER, DropAction(pos, P_SILVER), InvalidDropPiece, s0)
    })

    allDroppablePosition.foreach(pos => {
      testActionError(BLACK_PLAYER, DropAction(pos, P_SILVER), InvalidDropPiece, s1)
    })
  }