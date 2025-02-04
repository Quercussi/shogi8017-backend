package com.shogi8017.app.services.logics

import com.shogi8017.app.errors.{ExpectingPromotion, IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.LANCE
import com.shogi8017.app.services.logics.pieces.{Lance, Pawn, PromotedLance}
import com.shogi8017.app.services.logics.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class LanceTest extends AnyFunSuite:
  test("Lance should move like a Lance") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Lance(WHITE_PLAYER),
        Position(6, 6) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    (5 to 8).foreach(col => testMove(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, col)), Lance(WHITE_PLAYER), s0))
    (2 to 5).foreach(col => testMove(BLACK_PLAYER, MoveAction(Position(6, 6), Position(6, col)), Lance(BLACK_PLAYER), s1))
  }

  test("Lance should not move backward") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 6) -> Lance(WHITE_PLAYER),
        Position(6, 4) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    (1 to 5).foreach(col => testMoveError(WHITE_PLAYER, MoveAction(Position(4, 6), Position(4, col)), IllegalMove, s0))
    (5 to 9).foreach(col => testMoveError(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, col)), IllegalMove, s1))
  }

  test("Lance should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Lance(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Lance(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(5, 7)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testMoveError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(2, 4)), IllegalMove, newBoard2)
  }

  test("Lance should not be able to jump") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Lance(WHITE_PLAYER),
        Position(4, 5) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 6)), IllegalMove, s0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(4, 5), Position(4, 3)), IllegalMove, s1)
  }

  test("Lance should capture forward") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 2) -> Lance(WHITE_PLAYER),
        Position(4, 9) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val r0 = testMove(WHITE_PLAYER, MoveAction(Position(4, 2), Position(4, 9), true), PromotedLance(Player.WHITE_PLAYER), s0)
    assert(r0.piecesMap.size == 3)
    assert(r0.hands.get(WHITE_PLAYER).contains(Multiset(LANCE)))
    assert(r0.hands.get(BLACK_PLAYER).contains(Multiset.empty))

    val r1 = testMove(BLACK_PLAYER, MoveAction(Position(4, 9), Position(4, 2), false), Lance(Player.BLACK_PLAYER), s1)
    assert(r1.piecesMap.size == 3)
    assert(r1.hands.get(BLACK_PLAYER).contains(Multiset(LANCE)))
    assert(r1.hands.get(WHITE_PLAYER).contains(Multiset.empty))
  }

  test("Lance should not capture a piece of its own side") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 2) -> Lance(WHITE_PLAYER),
        Position(4, 8) -> Lance(WHITE_PLAYER),
        Position(6, 2) -> Lance(BLACK_PLAYER),
        Position(6, 8) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 2), Position(4, 8)), IllegalMove, s0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(6, 8), Position(6, 2)), IllegalMove, s1)
  }

  test("Lance should not capture backward") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 8) -> Lance(WHITE_PLAYER),
        Position(4, 2) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))


    testMoveError(WHITE_PLAYER, MoveAction(Position(4, 8), Position(4, 2)), IllegalMove, s0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(4, 2), Position(4, 8)), IllegalMove, s1)
  }

  test("Lance should promote when reaching the last rank") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 6) -> Lance(WHITE_PLAYER),
        Position(6, 4) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(2, 6), Position(2, 9), false), ExpectingPromotion, s0)
    testMove(WHITE_PLAYER, MoveAction(Position(2, 6), Position(2, 9), true), PromotedLance(WHITE_PLAYER), s0)

    testMoveError(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, 1), false), ExpectingPromotion, s1)
    testMove(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, 1), true), PromotedLance(BLACK_PLAYER), s1)
  }

  test("Lance should be able to promote when reaching the last three ranks") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 6) -> Lance(WHITE_PLAYER),
        Position(6, 4) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMove(WHITE_PLAYER, MoveAction(Position(2, 6), Position(2, 8), false), Lance(WHITE_PLAYER), s0)
    testMove(WHITE_PLAYER, MoveAction(Position(2, 6), Position(2, 8), true), PromotedLance(WHITE_PLAYER), s0)

    testMove(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, 2), false), Lance(BLACK_PLAYER), s1)
    testMove(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, 2), true), PromotedLance(BLACK_PLAYER), s1)
  }

  test("Lance cannot promote outside the last three rank") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(1, 4) -> Lance(WHITE_PLAYER),
        Position(2, 6) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testMoveError(WHITE_PLAYER, MoveAction(Position(1, 4), Position(1, 6), true), IncorrectPromotionScenario, s0)
    testMoveError(BLACK_PLAYER, MoveAction(Position(2, 6), Position(2, 4), true), IncorrectPromotionScenario, s1)
  }
  
  // TODO: drop tests