package com.shogi8017.app.services.logics

import com.shogi8017.app.exceptions.{ExpectingPromotion, IllegalDrop, IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.actions.{DropAction, MoveAction}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.KNIGHT
import com.shogi8017.app.services.logics.pieces.{Knight, Pawn, PromotedKnight}
import com.shogi8017.app.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class KnightTest extends AnyFunSuite:
  test("Knight should move like a knight") {
    val defaultBoard = Board.defaultInitialPosition
    val newPiece = defaultBoard.piecesMap
      - Position(3, 3)
      - Position(3, 7)

    val s0 = Board(newPiece)
    testAction(BLACK_PLAYER, MoveAction(Position(2, 9), Position(3, 7)), Knight(BLACK_PLAYER), s0)
  }

  test("Knight should not move backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Knight(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testActionError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(7, 8)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(3, 2)), IllegalMove, newBoard2)
  }

  test("Knight should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Knight(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testActionError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(2, 4)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(5, 7)), IllegalMove, newBoard2)
  }

  test("Knight should be able to jump") {
    val defaultBoard = Board.emptyBoard
    val newPieces = defaultBoard.piecesMap
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      + (Position(4, 5) -> Pawn(Player.WHITE_PLAYER))
      + (Position(5, 5) -> Pawn(Player.WHITE_PLAYER))
      + (Position(5, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(5, 3) -> Pawn(Player.WHITE_PLAYER))
      + (Position(4, 3) -> Pawn(Player.WHITE_PLAYER))
      + (Position(3, 3) -> Pawn(Player.WHITE_PLAYER))
      + (Position(3, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(3, 5) -> Pawn(Player.WHITE_PLAYER))

    val newBoard = Board(newPieces).copy(currentPlayerTurn = WHITE_PLAYER)
    testAction(WHITE_PLAYER, MoveAction(Position(4, 4), Position(5, 6)), Knight(WHITE_PLAYER), newBoard)
  }

  test("Knight should capture forward") {
    val s0 = Board.defaultInitialPosition.copy(
      piecesMap = Board.defaultInitialPosition.piecesMap ++ Map(
        Position(4, 4) -> Knight(WHITE_PLAYER),
        Position(3, 6) -> Knight(BLACK_PLAYER)
      ) -- Set(
        Position(4, 2),
        Position(4, 7)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val r0 = testAction(BLACK_PLAYER, MoveAction(Position(3, 6), Position(4, 4)), Knight(Player.BLACK_PLAYER), s0)
    assert(r0.piecesMap.size == 40)
    assert(r0.hands.get(BLACK_PLAYER).contains(Multiset(KNIGHT)))
    assert(r0.hands.get(WHITE_PLAYER).contains(Multiset.empty))

    val r1 = testAction(WHITE_PLAYER, MoveAction(Position(4, 4), Position(3, 6)), Knight(Player.WHITE_PLAYER), s1)
    assert(r1.piecesMap.size == 40)
    assert(r1.hands.get(WHITE_PLAYER).contains(Multiset(KNIGHT)))
    assert(r1.hands.get(BLACK_PLAYER).contains(Multiset.empty))
  }

  test("Knight should not capture a piece of its own side") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Knight(Player.WHITE_PLAYER))
      + (Position(3, 6) -> Knight(Player.WHITE_PLAYER))
      + (Position(5, 5) -> Knight(Player.BLACK_PLAYER))
      + (Position(6, 3) -> Knight(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testActionError(BLACK_PLAYER, MoveAction(Position(5, 5), Position(6, 3)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(3, 6)), IllegalMove, newBoard2)
  }

  test("Knight should not capture backward") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(3, 6) -> Knight(WHITE_PLAYER),
        Position(4, 4) -> Knight(BLACK_PLAYER)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(BLACK_PLAYER, MoveAction(Position(4, 4), Position(3, 6)), IllegalMove, s0)

    testActionError(WHITE_PLAYER, MoveAction(Position(3, 6), Position(4, 4)), IllegalMove, s1)
  }

  test("Knight should promote when reaching the second last rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(2, 6) -> Knight(WHITE_PLAYER))
      + (Position(6, 4) -> Knight(BLACK_PLAYER))
    val board0 = Board(newPieces)

    testActionError(BLACK_PLAYER, MoveAction(Position(6, 4), Position(7, 2), false), ExpectingPromotion, board0)
    val board1 = testAction(BLACK_PLAYER, MoveAction(Position(6, 4), Position(7, 2), true), PromotedKnight(BLACK_PLAYER), board0)

    testActionError(WHITE_PLAYER, MoveAction(Position(2, 6), Position(1, 8), false), ExpectingPromotion, board1)
    testAction(WHITE_PLAYER, MoveAction(Position(2, 6), Position(1, 8), true), PromotedKnight(WHITE_PLAYER), board1)
  }

  test("Knight cannot promote outside the last three rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(1, 4) -> Knight(WHITE_PLAYER))
      + (Position(2, 6) -> Knight(BLACK_PLAYER))
    val board0 = Board(newPieces)
    val board1 = board0.copy(auxiliaryState = board0.auxiliaryState, currentPlayerTurn = WHITE_PLAYER)

    testActionError(BLACK_PLAYER, MoveAction(Position(2, 6), Position(3, 4), true), IncorrectPromotionScenario, board0)
    testActionError(WHITE_PLAYER, MoveAction(Position(1, 4), Position(2, 6), true), IncorrectPromotionScenario, board1)
  }
  
  test("Gold should be able to drop any unoccupied position except the last two ranks") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(KNIGHT),
        BLACK_PLAYER -> Multiset(KNIGHT)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    def allDroppablePosition(player: Player) = allPositions
      .filterNot(s0.piecesMap.contains)
      .filterNot(p => if player == WHITE_PLAYER then Seq(8,9).contains(p.y) else Seq(1,2).contains(p.y))

    allDroppablePosition(BLACK_PLAYER).foreach(pos => {
      val r0 = testAction(BLACK_PLAYER, DropAction(pos, KNIGHT), Knight(BLACK_PLAYER), s0)
      assert(r0.piecesMap.size == 3)
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset(KNIGHT)))
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })

    allDroppablePosition(WHITE_PLAYER).foreach(pos => {
      val r1 = testAction(WHITE_PLAYER, DropAction(pos, KNIGHT), Knight(WHITE_PLAYER), s1)
      assert(r1.piecesMap.size == 3)
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset.empty))
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset(KNIGHT)))
    })
  }

  test("Knight should be able to drop on the last two ranks") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        WHITE_PLAYER -> Multiset(KNIGHT),
        BLACK_PLAYER -> Multiset(KNIGHT)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    def allUndroppablePosition(player: Player) = allPositions
      .filterNot(s0.piecesMap.contains)
      .filter(p => if player == WHITE_PLAYER then Seq(8,9).contains(p.y) else Seq(1,2).contains(p.y))

    allUndroppablePosition(BLACK_PLAYER).foreach(pos => {
      testActionError(BLACK_PLAYER, DropAction(pos, KNIGHT), IllegalDrop, s0)
    })

    allUndroppablePosition(WHITE_PLAYER).foreach(pos => {
      testActionError(WHITE_PLAYER, DropAction(pos, KNIGHT), IllegalDrop, s1)
    })
  }

  test("Knight score should be 1") {
    assert(Knight(WHITE_PLAYER).score == 1)
    assert(Knight(BLACK_PLAYER).score == 1)
  }