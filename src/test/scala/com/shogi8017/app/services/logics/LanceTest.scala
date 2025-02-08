package com.shogi8017.app.services.logics

import com.shogi8017.app.exceptions.{ExpectingPromotion, IllegalDrop, IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.LANCE
import com.shogi8017.app.services.logics.pieces.{Lance, PromotedLance}
import com.shogi8017.app.utils.Multiset
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

    (5 to 8).foreach(col => testAction(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, col)), Lance(WHITE_PLAYER), s0))
    (2 to 5).foreach(col => testAction(BLACK_PLAYER, MoveAction(Position(6, 6), Position(6, col)), Lance(BLACK_PLAYER), s1))
  }

  test("Lance should not move backward") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 6) -> Lance(WHITE_PLAYER),
        Position(6, 4) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    (1 to 5).foreach(col => testActionError(WHITE_PLAYER, MoveAction(Position(4, 6), Position(4, col)), IllegalMove, s0))
    (5 to 9).foreach(col => testActionError(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, col)), IllegalMove, s1))
  }

  test("Lance should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Lance(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Lance(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(5, 7)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, lastAction = Some(Action(WHITE_PLAYER)))
    testActionError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(2, 4)), IllegalMove, newBoard2)
  }

  test("Lance should not be able to jump") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 4) -> Lance(WHITE_PLAYER),
        Position(4, 5) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 6)), IllegalMove, s0)
    testActionError(BLACK_PLAYER, MoveAction(Position(4, 5), Position(4, 3)), IllegalMove, s1)
  }

  test("Lance should capture forward") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 2) -> Lance(WHITE_PLAYER),
        Position(4, 9) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val r0 = testAction(WHITE_PLAYER, MoveAction(Position(4, 2), Position(4, 9), true), PromotedLance(Player.WHITE_PLAYER), s0)
    assert(r0.piecesMap.size == 3)
    assert(r0.hands.get(WHITE_PLAYER).contains(Multiset(LANCE)))
    assert(r0.hands.get(BLACK_PLAYER).contains(Multiset.empty))

    val r1 = testAction(BLACK_PLAYER, MoveAction(Position(4, 9), Position(4, 2), false), Lance(Player.BLACK_PLAYER), s1)
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

    testActionError(WHITE_PLAYER, MoveAction(Position(4, 2), Position(4, 8)), IllegalMove, s0)
    testActionError(BLACK_PLAYER, MoveAction(Position(6, 8), Position(6, 2)), IllegalMove, s1)
  }

  test("Lance should not capture backward") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(4, 8) -> Lance(WHITE_PLAYER),
        Position(4, 2) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))


    testActionError(WHITE_PLAYER, MoveAction(Position(4, 8), Position(4, 2)), IllegalMove, s0)
    testActionError(BLACK_PLAYER, MoveAction(Position(4, 2), Position(4, 8)), IllegalMove, s1)
  }

  test("Lance should promote when reaching the last rank") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 6) -> Lance(WHITE_PLAYER),
        Position(6, 4) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testActionError(WHITE_PLAYER, MoveAction(Position(2, 6), Position(2, 9), false), ExpectingPromotion, s0)
    testAction(WHITE_PLAYER, MoveAction(Position(2, 6), Position(2, 9), true), PromotedLance(WHITE_PLAYER), s0)

    testActionError(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, 1), false), ExpectingPromotion, s1)
    testAction(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, 1), true), PromotedLance(BLACK_PLAYER), s1)
  }

  test("Lance should be able to promote when reaching the last three ranks") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(2, 6) -> Lance(WHITE_PLAYER),
        Position(6, 4) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testAction(WHITE_PLAYER, MoveAction(Position(2, 6), Position(2, 8), false), Lance(WHITE_PLAYER), s0)
    testAction(WHITE_PLAYER, MoveAction(Position(2, 6), Position(2, 8), true), PromotedLance(WHITE_PLAYER), s0)

    testAction(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, 2), false), Lance(BLACK_PLAYER), s1)
    testAction(BLACK_PLAYER, MoveAction(Position(6, 4), Position(6, 2), true), PromotedLance(BLACK_PLAYER), s1)
  }

  test("Lance cannot promote outside the last three rank") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(1, 4) -> Lance(WHITE_PLAYER),
        Position(2, 6) -> Lance(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    testActionError(WHITE_PLAYER, MoveAction(Position(1, 4), Position(1, 6), true), IncorrectPromotionScenario, s0)
    testActionError(BLACK_PLAYER, MoveAction(Position(2, 6), Position(2, 4), true), IncorrectPromotionScenario, s1)
  }

  test("Lance should be able to drop any unoccupied position except the last rank") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(LANCE),
        BLACK_PLAYER -> Multiset(LANCE)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    def allDroppablePosition(player: Player) = allPositions
      .filterNot(s0.piecesMap.contains)
      .filterNot(if player == WHITE_PLAYER then _.y == 9 else _.y == 1)


    allDroppablePosition(WHITE_PLAYER).foreach(pos => {
      val r0 = testAction(WHITE_PLAYER, DropAction(pos, LANCE), Lance(WHITE_PLAYER), s0)
      assert(r0.piecesMap.size == 3)
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset.empty))
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset(LANCE)))
    })

    allDroppablePosition(BLACK_PLAYER).foreach(pos => {
      val r1 = testAction(BLACK_PLAYER, DropAction(pos, LANCE), Lance(BLACK_PLAYER), s1)
      assert(r1.piecesMap.size == 3)
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset(LANCE)))
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })
  }

  test("Lance should be able to drop on the last rank") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(LANCE),
        BLACK_PLAYER -> Multiset(LANCE)
      )
    )
    val s1 = s0.copy(lastAction = Some(Action(WHITE_PLAYER)))

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    def allUndroppablePosition(player: Player) = allPositions
      .filterNot(s0.piecesMap.contains)
      .filter(if player == WHITE_PLAYER then _.y == 9 else _.y == 1)

    allUndroppablePosition(WHITE_PLAYER).foreach(pos => {
      testActionError(WHITE_PLAYER, DropAction(pos, LANCE), IllegalDrop, s0)
    })

    allUndroppablePosition(BLACK_PLAYER).foreach(pos => {
      testActionError(BLACK_PLAYER, DropAction(pos, LANCE), IllegalDrop, s1)
    })
  }