package com.shogi8017.app.services.logics

import com.shogi8017.app.exceptions.{ExpectingPromotion, IllegalDrop, IllegalMove, IncorrectPromotionScenario}
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.actions.{DropAction, MoveAction}
import com.shogi8017.app.services.logics.pieces.PromotablePieceType.{LANCE, PAWN}
import com.shogi8017.app.services.logics.pieces.{Bishop, Gold, King, Lance, Pawn, PromotedPawn, Rook}
import com.shogi8017.app.utils.Multiset
import org.scalatest.funsuite.AnyFunSuite

class PawnTest extends AnyFunSuite:
  test("Pawn should move forward by one square") {
    val s1 = testAction(BLACK_PLAYER, MoveAction(Position(2, 7), Position(2, 6)), Pawn(BLACK_PLAYER))
    testAction(WHITE_PLAYER, MoveAction(Position(2, 3), Position(2, 4)), Pawn(WHITE_PLAYER), s1)
  }

  test("Pawn should not move backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Pawn(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testActionError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(4, 7)), IllegalMove, newBoard1)

    val newBoard2 =Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 3)), IllegalMove, newBoard2)
  }

  test("Pawn should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(6, 6) -> Pawn(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testActionError(BLACK_PLAYER, MoveAction(Position(6, 6), Position(2, 4)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(5, 6)), IllegalMove, newBoard2)
  }

  test("Pawn cannot move forward when blocked") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.piecesMap
      - Position(4, 2)
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      - Position(4, 1)
      + (Position(4, 5) -> Gold(Player.WHITE_PLAYER))

    val newBoard = Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 5)), IllegalMove, newBoard)
  }

  test("Pawn should capture forward") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.piecesMap
      - Position(4, 2)
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      - Position(4, 7)
      + (Position(4, 5) -> Pawn(Player.BLACK_PLAYER))

    val newBoard = Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testAction(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 5)), Pawn(Player.WHITE_PLAYER), newBoard)
  }

  test("Pawn should not capture a piece of its own side") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(4, 4) -> Pawn(Player.WHITE_PLAYER))
      + (Position(4, 5) -> Pawn(Player.WHITE_PLAYER))
      + (Position(5, 5) -> Pawn(Player.BLACK_PLAYER))
      + (Position(5, 4) -> Pawn(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testActionError(BLACK_PLAYER, MoveAction(Position(5, 5), Position(5, 4)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 4), Position(4, 5)), IllegalMove, newBoard2)
  }

  test("Pawn should not capture backward") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      - Position(4, 2)
      + (Position(4, 6) -> Pawn(Player.WHITE_PLAYER))
      - Position(3, 7)
      + (Position(3, 5) -> Pawn(Player.BLACK_PLAYER))

    val newBoard1 = Board(newPieces)
    testActionError(BLACK_PLAYER, MoveAction(Position(3, 5), Position(4, 6)), IllegalMove, newBoard1)

    val newBoard2 = Board(piecesMap = newPieces, currentPlayerTurn = WHITE_PLAYER)
    testActionError(WHITE_PLAYER, MoveAction(Position(4, 6), Position(3, 5)), IllegalMove, newBoard2)
  }

  test("Pawn should promote when reaching the last rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(1, 8) -> Pawn(WHITE_PLAYER))
      + (Position(1, 2) -> Pawn(BLACK_PLAYER))
    val board0 = Board(newPieces)

    testActionError(BLACK_PLAYER, MoveAction(Position(1, 2), Position(1, 1), false), ExpectingPromotion, board0)
    val board1 = testAction(BLACK_PLAYER, MoveAction(Position(1, 2), Position(1, 1), true), PromotedPawn(BLACK_PLAYER), board0)

    testActionError(WHITE_PLAYER, MoveAction(Position(1, 8), Position(1, 9), false), ExpectingPromotion, board1)
    testAction(WHITE_PLAYER, MoveAction(Position(1, 8), Position(1, 9), true), PromotedPawn(WHITE_PLAYER), board1)
  }

  test("Pawn cannot promote outside the last three rank") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.piecesMap
      + (Position(1, 5) -> Pawn(WHITE_PLAYER))
      + (Position(2, 5) -> Pawn(BLACK_PLAYER))
    val board0 = Board(newPieces)
    val board1 = board0.copy(currentPlayerTurn = WHITE_PLAYER)

    testActionError(BLACK_PLAYER, MoveAction(Position(2, 5), Position(2, 4), true), IncorrectPromotionScenario, board0)
    testActionError(WHITE_PLAYER, MoveAction(Position(1, 5), Position(1, 6), true), IncorrectPromotionScenario, board1)
  }

  test("Lance should be able to drop any unoccupied position except the last rank") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        BLACK_PLAYER -> Multiset(LANCE),
        WHITE_PLAYER -> Multiset(LANCE)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    def allDroppablePosition(player: Player) = allPositions
      .filterNot(s0.piecesMap.contains)
      .filterNot(if player == WHITE_PLAYER then _.y == 9 else _.y == 1)

    allDroppablePosition(BLACK_PLAYER).foreach(pos => {
      val r1 = testAction(BLACK_PLAYER, DropAction(pos, LANCE), Lance(BLACK_PLAYER), s0)
      assert(r1.piecesMap.size == 3)
      assert(r1.hands.get(WHITE_PLAYER).contains(Multiset(LANCE)))
      assert(r1.hands.get(BLACK_PLAYER).contains(Multiset.empty))
    })

    allDroppablePosition(WHITE_PLAYER).foreach(pos => {
      val r0 = testAction(WHITE_PLAYER, DropAction(pos, LANCE), Lance(WHITE_PLAYER), s1)
      assert(r0.piecesMap.size == 3)
      assert(r0.hands.get(WHITE_PLAYER).contains(Multiset.empty))
      assert(r0.hands.get(BLACK_PLAYER).contains(Multiset(LANCE)))
    })
  }

  test("Pawn should be able to drop on the last rank") {
    val s0 = Board.emptyBoard.copy(
      hands = Map(
        BLACK_PLAYER -> Multiset(PAWN),
        WHITE_PLAYER -> Multiset(PAWN)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    val allPositions = for {
      row <- 1 to 9
      col <- 1 to 9
    } yield Position(row, col)

    def allUndroppablePosition(player: Player) = allPositions
      .filterNot(s0.piecesMap.contains)
      .filter(if player == WHITE_PLAYER then _.y == 9 else _.y == 1)

    allUndroppablePosition(BLACK_PLAYER).foreach(pos => {
      testActionError(BLACK_PLAYER, DropAction(pos, PAWN), IllegalDrop, s0)
    })

    allUndroppablePosition(WHITE_PLAYER).foreach(pos => {
      testActionError(WHITE_PLAYER, DropAction(pos, PAWN), IllegalDrop, s1)
    })
  }

  test("Pawn cannot drop such that it ends in a checkmate") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Map(
        Position(5,2) -> King(WHITE_PLAYER),
        Position(4,6) -> Pawn(WHITE_PLAYER),
        Position(3,7) -> Bishop(WHITE_PLAYER),
        Position(6,7) -> Rook(WHITE_PLAYER),
        Position(6,9) -> Rook(WHITE_PLAYER),

        Position(5,8) -> King(BLACK_PLAYER),
        Position(6,4) -> Pawn(BLACK_PLAYER),
        Position(4,3) -> Rook(BLACK_PLAYER),
        Position(6,3) -> Rook(BLACK_PLAYER),
        Position(4,1) -> Rook(BLACK_PLAYER),
      ),
      hands = Map(
        BLACK_PLAYER -> Multiset(PAWN), // Black player plays s0
        WHITE_PLAYER -> Multiset(PAWN)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    //  a b c d e f g h i
    //9 . . . . . R . . .
    //8 . . . . k . . . .
    //7 . . B . . R . . .
    //6 . . . P . . . . .
    //5 . . . . . . . . .
    //4 . . . . . p . . .
    //3 . . . r . r . . .
    //2 . . . . K . . . .
    //1 . . . r . . . . .

    testActionError(BLACK_PLAYER, DropAction(Position(5, 3), PAWN), IllegalDrop, s0)
    testActionError(WHITE_PLAYER, DropAction(Position(5, 7), PAWN), IllegalDrop, s1)
  }

  test("Pawn cannot drop on a file already occupied by another pawn of the same player") {
    val s0 = Board.emptyBoard.copy(
      piecesMap = Board.emptyBoard.piecesMap ++ Map(
        Position(3, 7) -> Pawn(WHITE_PLAYER),
        Position(4, 8) -> Pawn(BLACK_PLAYER),
      ),
      hands = Map(
        WHITE_PLAYER -> Multiset(PAWN),
        BLACK_PLAYER -> Multiset(PAWN)
      )
    )
    val s1 = s0.copy(currentPlayerTurn = WHITE_PLAYER)

    //  a b c d e f g h i
    //9 . . . . k . . . .
    //8 . . . p . . . . .
    //7 . . P . . . . . .
    //6 . . . . . . . . .
    //5 . . . . . . . . .
    //4 . . . . . . . . .
    //3 . . . . . . . . .
    //2 . . . . . . . . .
    //1 . . . . K . . . .

    testActionError(BLACK_PLAYER, DropAction(Position(4, 5), PAWN), IllegalDrop, s0)
    testActionError(WHITE_PLAYER, DropAction(Position(3, 4), PAWN), IllegalDrop, s1)
  }

  test("Pawn score should be 1") {
    assert(Pawn(WHITE_PLAYER).score == 1)
    assert(Pawn(BLACK_PLAYER).score == 1)
  }