package com.shogi8017.app.services.logics

import com.shogi8017.app.errors.{CannotPromote, IllegalMove, InvalidDropPiece}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.{BISHOP, PAWN}
import com.shogi8017.app.services.logics.pieces.PromotedPieceType.P_BISHOP
import com.shogi8017.app.services.logics.pieces.{Knight, PromotedBishop, PromotedPawn}
import com.shogi8017.app.services.logics.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class PromotedBishopTest extends AnyFunSuite:
  private def generateReachablePositions(start: Position): Seq[Position] = {
    val rangedDirections = Seq((-1, -1), (1, 1), (-1, 1), (1, -1))
    val unitDirections = Seq(Direction(-1, 0), Direction(1, 0), Direction(0, 1), Direction(0, -1))
    val range = 1 to 9

    val rangedPositions = rangedDirections.flatMap { case (dx, dy) =>
      range.map(i => Position(start.x + i * dx, start.y + i * dy))
    }

    val unitPositions = unitDirections.map(start.move)

    (rangedPositions ++ unitPositions).filter(!_.isOutOfBoard)
  }
  
  test("A PromotedBishop should move in ranged diagonal or unit cross") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedBishop(WHITE_PLAYER),
        Position(4, 6) -> PromotedBishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite = generateReachablePositions(Position(4, 4))
    testSeqWhite.foreach(pos => testAction(WHITE_PLAYER, MoveAction(Position(4, 4), pos), PromotedBishop(WHITE_PLAYER), s0))

    val testSeqBlack = generateReachablePositions(Position(4, 6))
    testSeqBlack.foreach(pos => testAction(BLACK_PLAYER, MoveAction(Position(4, 6), pos), PromotedBishop(BLACK_PLAYER), s1))
  }

  test("A PromotedBishop should not move like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedBishop(WHITE_PLAYER),
        Position(4, 6) -> PromotedBishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val whiteReachablePositions = generateReachablePositions(Position(4, 4))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), testSeqWhite.head), IllegalMove, s0)

    val blackReachablePositions = generateReachablePositions(Position(4, 6))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains)
    testActionError(BLACK_PLAYER, MoveAction(Position(4, 6), testSeqBlack.head), IllegalMove, s1)
  }

  test("A PromotedBishop should not be able to jump") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedBishop(WHITE_PLAYER),
        Position(7, 7) -> PromotedBishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(8, 8)), IllegalMove, s0)
    testActionError(BLACK_PLAYER, MoveAction(Position(7, 7), Position(2, 2)), IllegalMove, s1)
  }

  test("A PromotedBishop should capture in ranged diagonal or unit cross") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedBishop(WHITE_PLAYER),
        Position(6, 7) -> PromotedBishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite = generateReachablePositions(Position(4, 4))
    testSeqWhite.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> PromotedBishop(BLACK_PLAYER)))
      val r0 = testAction(WHITE_PLAYER, MoveAction(Position(4, 4), pos), PromotedBishop(WHITE_PLAYER), s0_temp)
      assert(r0.piecesMap.size == 4)
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset(BISHOP)))
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })

    val testSeqBlack = generateReachablePositions(Position(6, 7))
    testSeqBlack.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> PromotedPawn(WHITE_PLAYER)))
      val r1 = testAction(BLACK_PLAYER, MoveAction(Position(6, 7), pos), PromotedBishop(BLACK_PLAYER), s1_temp)
      assert(r1.piecesMap.size == 4)
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset(PAWN)))
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset.empty))
    })
  }

  test("A PromotedBishop should not capture a piece of its own side") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedBishop(WHITE_PLAYER),
        Position(4, 6) -> PromotedBishop(BLACK_PLAYER),
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite = generateReachablePositions(Position(4, 4))
    testSeqWhite.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), pos), IllegalMove, s0_temp)
    })

    val testSeqBlack = generateReachablePositions(Position(4, 6))
    testSeqBlack.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(4, 6), pos), IllegalMove, s1_temp)
    })
  }

  test("A PromotedBishop should not capture like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedBishop(WHITE_PLAYER),
        Position(4, 6) -> PromotedBishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val whiteReachablePositions = generateReachablePositions(Position(4, 4))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains).filterNot(p => p == Position(4, 4) || p == Position(5, 1) || p == Position(5, 9))
    testSeqWhite.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), pos), IllegalMove, s0_temp)
    })

    val blackReachablePositions = generateReachablePositions(Position(4, 6))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains).filterNot(p => p == Position(4, 6) || p == Position(5, 1) || p == Position(5, 9))
    testSeqBlack.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(4, 6), pos), IllegalMove, s1_temp)
    })
  }

  test("A PromotedBishop should be able to promote") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 5) -> PromotedBishop(WHITE_PLAYER),
        Position(2, 7) -> PromotedBishop(WHITE_PLAYER),
        Position(8, 5) -> PromotedBishop(BLACK_PLAYER),
        Position(8, 3) -> PromotedBishop(BLACK_PLAYER),
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    generateReachablePositions(Position(2, 5)).filter(_.y >= 7).foreach(pos =>
      testActionError(WHITE_PLAYER, MoveAction(Position(2, 5), pos, true), CannotPromote, s0)
    )

    generateReachablePositions(Position(2, 7)).foreach(pos =>
      testActionError(WHITE_PLAYER, MoveAction(Position(2, 7), pos, true), CannotPromote, s0)
    )

    generateReachablePositions(Position(8, 5)).filter(_.y <= 3).foreach(pos =>
      testActionError(BLACK_PLAYER, MoveAction(Position(8, 5), pos, true), CannotPromote, s1)
    )

    generateReachablePositions(Position(8, 3)).foreach(pos =>
      testActionError(BLACK_PLAYER, MoveAction(Position(8, 3), pos, true), CannotPromote, s1)
    )
  }

  test("Promoted Bishop should not be able to be dropped") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(P_BISHOP),
        BLACK_PLAYER -> Multiset(P_BISHOP)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    val allDroppablePosition = allPositions.filterNot(s0.piecesMap.contains)

    allDroppablePosition.foreach(pos => {
      testActionError(WHITE_PLAYER, DropAction(pos, P_BISHOP), InvalidDropPiece, s0)
    })

    allDroppablePosition.foreach(pos => {
      testActionError(BLACK_PLAYER, DropAction(pos, P_BISHOP), InvalidDropPiece, s1)
    })
  }