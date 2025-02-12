package com.shogi8017.app.services.logics

import com.shogi8017.app.exceptions.{CannotPromote, IllegalMove, InvalidDropPiece}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.actions.{DropAction, MoveAction}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.{PAWN, ROOK}
import com.shogi8017.app.services.logics.pieces.PromotedPieceType.P_ROOK
import com.shogi8017.app.services.logics.pieces.{Knight, PromotedPawn, PromotedRook}
import com.shogi8017.app.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class PromotedRookTest extends AnyFunSuite:
  private def generateReachablePositions(start: Position): Seq[Position] = {
    val rangedDirections = Seq((-1, 0), (1, 0), (0, 1), (0, -1))
    val unitDirections = Seq(Direction(-1, -1), Direction(1, -1), Direction(-1, 1), Direction(1, 1))
    val range = 1 to 9

    val rangedPositions = rangedDirections.flatMap { case (dx, dy) =>
      range.map(i => Position(start.x + i * dx, start.y + i * dy))
    }

    val unitPositions = unitDirections.map(start.move)

    (rangedPositions ++ unitPositions).filter(!_.isOutOfBoard)
  }
  
  test("A PromotedRook should move in ranged diagonal or unit cross") {
    val whiteRookPos = Position(4, 4)
    val blackRookPos = Position(3, 5)
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        whiteRookPos -> PromotedRook(WHITE_PLAYER),
        blackRookPos -> PromotedRook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(auxiliaryState = s0.auxiliaryState.copy(lastAction = Some(Actor(BLACK_PLAYER))))

    val testSeqBlack = generateReachablePositions(blackRookPos)
    testSeqBlack.foreach(pos => testAction(BLACK_PLAYER, MoveAction(blackRookPos, pos), PromotedRook(BLACK_PLAYER), s0))

    val testSeqWhite = generateReachablePositions(whiteRookPos)
    testSeqWhite.foreach(pos => testAction(WHITE_PLAYER, MoveAction(whiteRookPos, pos), PromotedRook(WHITE_PLAYER), s1))
  }

  test("A PromotedRook should not move like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedRook(WHITE_PLAYER),
        Position(3, 5) -> PromotedRook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(auxiliaryState = s0.auxiliaryState.copy(lastAction = Some(Actor(BLACK_PLAYER))))

    val blackReachablePositions = generateReachablePositions(Position(3, 5))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains)
    testActionError(BLACK_PLAYER, MoveAction(Position(3, 5), testSeqBlack.head), IllegalMove, s0)

    val whiteReachablePositions = generateReachablePositions(Position(4, 4))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), testSeqWhite.head), IllegalMove, s1)
  }

  test("A PromotedRook should not be able to jump") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedRook(WHITE_PLAYER),
        Position(7, 4) -> PromotedRook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(auxiliaryState = s0.auxiliaryState.copy(lastAction = Some(Actor(BLACK_PLAYER))))

    testActionError(BLACK_PLAYER, MoveAction(Position(7, 4), Position(2, 4)), IllegalMove, s0)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(8, 4)), IllegalMove, s1)
  }

  test("A PromotedRook should capture in ranged diagonal or unit cross") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedRook(WHITE_PLAYER),
        Position(6, 7) -> PromotedRook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(auxiliaryState = s0.auxiliaryState.copy(lastAction = Some(Actor(BLACK_PLAYER))))

    val testSeqBlack = generateReachablePositions(Position(6, 7))
    testSeqBlack.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> PromotedPawn(WHITE_PLAYER)))
      val r0 = testAction(BLACK_PLAYER, MoveAction(Position(6, 7), pos), PromotedRook(BLACK_PLAYER), s0_temp)
      assert(r0.piecesMap.size == 4)
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset(PAWN)))
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset.empty))
    })

    val testSeqWhite = generateReachablePositions(Position(4, 4))
    testSeqWhite.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> PromotedRook(BLACK_PLAYER)))
      val r1 = testAction(WHITE_PLAYER, MoveAction(Position(4, 4), pos), PromotedRook(WHITE_PLAYER), s1_temp)
      assert(r1.piecesMap.size == 4)
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset(ROOK)))
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })
  }

  test("A PromotedRook should not capture a piece of its own side") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedRook(WHITE_PLAYER),
        Position(3, 5) -> PromotedRook(BLACK_PLAYER),
      )
    )
    val s1 = s0.copy(auxiliaryState = s0.auxiliaryState.copy(lastAction = Some(Actor(BLACK_PLAYER))))

    val testSeqBlack = generateReachablePositions(Position(3, 5))
    testSeqBlack.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(3, 5), pos), IllegalMove, s0_temp)
    })

    val testSeqWhite = generateReachablePositions(Position(4, 4))
    testSeqWhite.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), pos), IllegalMove, s1_temp)
    })
  }

  test("A PromotedRook should not capture like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> PromotedRook(WHITE_PLAYER),
        Position(3, 5) -> PromotedRook(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(auxiliaryState = s0.auxiliaryState.copy(lastAction = Some(Actor(BLACK_PLAYER))))

    val blackReachablePositions = generateReachablePositions(Position(3, 5))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains).filterNot(p => p == Position(3, 5) || p == Position(5, 1) || p == Position(5, 9))
    testSeqBlack.foreach(pos => {
      val s0_temp = s0.copy(piecesMap = s0.piecesMap + (pos -> Knight(WHITE_PLAYER)))
      testActionError(BLACK_PLAYER, MoveAction(Position(3, 5), pos), IllegalMove, s0_temp)
    })

    val whiteReachablePositions = generateReachablePositions(Position(4, 4))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains).filterNot(p => p == Position(4, 4) || p == Position(5, 1) || p == Position(5, 9))
    testSeqWhite.foreach(pos => {
      val s1_temp = s1.copy(piecesMap = s1.piecesMap + (pos -> Knight(BLACK_PLAYER)))
      testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), pos), IllegalMove, s1_temp)
    })
  }

  test("A PromotedRook should not be able to promote") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 5) -> PromotedRook(WHITE_PLAYER),
        Position(3, 7) -> PromotedRook(WHITE_PLAYER),
        Position(8, 5) -> PromotedRook(BLACK_PLAYER),
        Position(7, 3) -> PromotedRook(BLACK_PLAYER),
      )
    )
    val s1 = s0.copy(auxiliaryState = s0.auxiliaryState.copy(lastAction = Some(Actor(BLACK_PLAYER))))

    generateReachablePositions(Position(8, 5)).filter(_.y <= 3).foreach(pos =>
      testActionError(BLACK_PLAYER, MoveAction(Position(8, 5), pos, true), CannotPromote, s0)
    )

    generateReachablePositions(Position(7, 3)).foreach(pos =>
      testActionError(BLACK_PLAYER, MoveAction(Position(7, 3), pos, true), CannotPromote, s0)
    )

    generateReachablePositions(Position(2, 5)).filter(_.y >= 7).foreach(pos =>
      testActionError(WHITE_PLAYER, MoveAction(Position(2, 5), pos, true), CannotPromote, s1)
    )

    generateReachablePositions(Position(3, 7)).foreach(pos =>
      testActionError(WHITE_PLAYER, MoveAction(Position(3, 7), pos, true), CannotPromote, s1)
    )
  }

  test("Promoted Rook should not be able to be dropped") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(P_ROOK),
        BLACK_PLAYER -> Multiset(P_ROOK)
      )
    )
    val s1 = s0.copy(auxiliaryState = s0.auxiliaryState.copy(lastAction = Some(Actor(BLACK_PLAYER))))

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    val allDroppablePosition = allPositions.filterNot(s0.piecesMap.contains)

    allDroppablePosition.foreach(pos => {
      testActionError(BLACK_PLAYER, DropAction(pos, P_ROOK), InvalidDropPiece, s0)
    })

    allDroppablePosition.foreach(pos => {
      testActionError(WHITE_PLAYER, DropAction(pos, P_ROOK), InvalidDropPiece, s1)
    })
  }

  test("Promoted Rook score should be 5") {
    assert(PromotedRook(WHITE_PLAYER).score == 5)
    assert(PromotedRook(BLACK_PLAYER).score == 5)
  }