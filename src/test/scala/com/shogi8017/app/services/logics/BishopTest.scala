package com.shogi8017.app.services.logics

import com.shogi8017.app.errors.{IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.BISHOP
import com.shogi8017.app.services.logics.pieces.{Bishop, Knight, PromotedBishop}
import com.shogi8017.app.services.logics.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class BishopTest extends AnyFunSuite:
  private def generateDiagonalPositions(start: Position): Seq[Position] = {
    val directions = Seq((-1, -1), (1, 1), (-1, 1), (1, -1))
    val range = 1 to 9
    directions
      .flatMap { case (dx, dy) => range.map(i => Position(start.x + i * dx, start.y + i * dy)) }
      .filter(pos => pos.x >= 1 && pos.x <= 9 && pos.y >= 1 && pos.y <= 9 && pos != start)
  }

  test("A Bishop should move diagonally") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Bishop(WHITE_PLAYER),
        Position(4, 6) -> Bishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite = generateDiagonalPositions(Position(4, 4))
    testSeqWhite.foreach(pos => testMove(WHITE_PLAYER, MoveAction(Position(4, 4), pos), Bishop(WHITE_PLAYER), s0))

    val testSeqBlack = generateDiagonalPositions(Position(4, 6))
    testSeqBlack.foreach(pos => testMove(BLACK_PLAYER, MoveAction(Position(4, 6), pos), Bishop(BLACK_PLAYER), s1))
  }

  test("A Bishop should not move like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Bishop(WHITE_PLAYER),
        Position(4, 6) -> Bishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val whiteReachablePositions = generateDiagonalPositions(Position(4, 4))
    val testSeqWhite = getAllPosition.filterNot(whiteReachablePositions.contains)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), testSeqWhite.head), IllegalMove, s0)

    val blackReachablePositions = generateDiagonalPositions(Position(4, 6))
    val testSeqBlack = getAllPosition.filterNot(blackReachablePositions.contains)
    testMoveError(BLACK_PLAYER, MoveAction(Position(4, 6), testSeqBlack.head), IllegalMove, s1)
  }

  test("A Bishop should be able to jump") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Bishop(WHITE_PLAYER),
        Position(7, 7) -> Bishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(8, 8)), IllegalMove, s0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(7, 7), Position(2, 2)), IllegalMove, s1)
  }

  test("A Bishop should capture like a cross") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Bishop(WHITE_PLAYER),
        Position(7, 7) -> Bishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val r1 = testMove(WHITE_PLAYER, MoveAction(Position(4, 4), Position(7, 7)), Bishop(WHITE_PLAYER), s0)
    assert(r1.piecesMap.size == 3)
    assert(r1.hands.get(WHITE_PLAYER).contains(Multiset(BISHOP)))
    assert(r1.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    
    val r2 = testMove(BLACK_PLAYER, MoveAction(Position(7, 7), Position(4, 4)), Bishop(BLACK_PLAYER), s1)
    assert(r2.piecesMap.size == 3)
    assert(r2.hands.get(BLACK_PLAYER).contains(Multiset(BISHOP)))
    assert(r2.hands.get(WHITE_PLAYER).contains(Multiset.empty))
  }

  test("A Bishop should not capture a piece of its own side") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Bishop(WHITE_PLAYER),
        Position(6, 6) -> Knight(WHITE_PLAYER),
        Position(4, 6) -> Bishop(BLACK_PLAYER),
        Position(2, 6) -> Knight(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(6, 6)), IllegalMove, s0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(4, 6), Position(2, 6)), IllegalMove, s1)
  }

  test("A Bishop should not capture like something else") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Bishop(WHITE_PLAYER),
        Position(4, 6) -> Bishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 6)), IllegalMove, s0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(4, 6), Position(4, 4)), IllegalMove, s1)
  }

  test("A Bishop should be able to promote when reaching or leaving the last three ranks") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(1, 6) -> Bishop(WHITE_PLAYER),
        Position(1, 9) -> Bishop(WHITE_PLAYER),
        Position(1, 4) -> Bishop(BLACK_PLAYER),
        Position(1, 1) -> Bishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite1 = generateDiagonalPositions(Position(1, 6)).filter(_.y >= 7)
    testSeqWhite1.foreach(pos => testMove(WHITE_PLAYER, MoveAction(Position(1, 6), pos, false), Bishop(WHITE_PLAYER), s0))
    testSeqWhite1.foreach(pos => testMove(WHITE_PLAYER, MoveAction(Position(1, 6), pos, true),  PromotedBishop(WHITE_PLAYER), s0))

    val testSeqWhite2 = generateDiagonalPositions(Position(1, 9))
    testSeqWhite2.foreach(pos => testMove(WHITE_PLAYER, MoveAction(Position(1, 9), pos, false), Bishop(WHITE_PLAYER), s0))
    testSeqWhite2.foreach(pos => testMove(WHITE_PLAYER, MoveAction(Position(1, 9), pos, true),  PromotedBishop(WHITE_PLAYER), s0))

    val testSeqBlack1 = generateDiagonalPositions(Position(1, 4)).filter(_.y <= 3)
    testSeqBlack1.foreach(pos => testMove(BLACK_PLAYER, MoveAction(Position(1, 4), pos, false), Bishop(BLACK_PLAYER), s1))
    testSeqBlack1.foreach(pos => testMove(BLACK_PLAYER, MoveAction(Position(1, 4), pos, true),  PromotedBishop(BLACK_PLAYER), s1))

    val testSeqBlack2 = generateDiagonalPositions(Position(1, 1))
    testSeqBlack2.foreach(pos => testMove(BLACK_PLAYER, MoveAction(Position(1, 1), pos, false), Bishop(BLACK_PLAYER), s1))
    testSeqBlack2.foreach(pos => testMove(BLACK_PLAYER, MoveAction(Position(1, 1), pos, true),  PromotedBishop(BLACK_PLAYER), s1))
  }

  test("Bishop cannot promote outside the last three rank") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Bishop(WHITE_PLAYER),
        Position(4, 6) -> Bishop(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val testSeqWhite = generateDiagonalPositions(Position(4, 4)).filter(_.y <= 6)
    testSeqWhite.foreach(pos => testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), pos, true), IncorrectPromotionScenario, s0))

    val testSeqBlack = generateDiagonalPositions(Position(4, 6)).filter(_.y >= 4)
    testSeqBlack.foreach(pos => testMoveError(BLACK_PLAYER, MoveAction(Position(4, 6), pos, true), IncorrectPromotionScenario, s1))
  }

// TODO: drop tests