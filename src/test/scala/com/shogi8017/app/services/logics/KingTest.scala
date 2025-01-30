package com.shogi8017.app.services.logics

import com.shogi8017.app.errors.IllegalMove
import com.shogi8017.app.services.logics.LogicTestUtils.*
import com.shogi8017.app.services.logics.Player.*
import org.scalatest.funsuite.AnyFunSuite

class KingTest extends AnyFunSuite:
  test("A King should move like a unit star") {
    val aroundMoves = for {
      dx <- -1 to 1
      dy <- -1 to 1
      if dx != 0 || dy != 0
    } yield (dx, dy)

    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      - Position(5, 1)
      - Position(5, 8)
      + (Position(4, 4) -> King(WHITE_PLAYER, true))
      + (Position(7, 7) -> King(BLACK_PLAYER, true))


    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    aroundMoves.foreach((x, y) =>
      testMove(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), King(WHITE_PLAYER, true), newBoard1)
    )

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(3, 4), Position(4, 4), King(WHITE_PLAYER, true), None)))
    val blackPos = Position(7,7)
    aroundMoves.foreach((x, y) => testMove(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(x,y)), King(BLACK_PLAYER, true), newBoard2))
  }

  test("A King should not be checked after a move") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      - Position(5, 1)
      - Position(5, 8)
      + (Position(4, 4) -> King(WHITE_PLAYER, true))
      + (Position(6, 5) -> King(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(4,4)
    testMoveError(WHITE_PLAYER, PlayerAction(whitePos, Position(5, 5)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(3, 4), Position(4, 4), King(WHITE_PLAYER, true), None)))
    val blackPos = Position(6,5)
    testMoveError(WHITE_PLAYER, PlayerAction(whitePos, Position(5, 5)), IllegalMove, newBoard1)
  }

  test("A king should not move like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      - Position(5, 1)
      - Position(5, 8)
      + (Position(2, 2) -> King(WHITE_PLAYER, true))
      + (Position(7, 7) -> King(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(2,2)
    val testingMoves1 = List((2,1),(2,-1),(-1,5),(-1,3),(1,2),(-1,2))
    testingMoves1.foreach((x, y) => testMoveError(WHITE_PLAYER, PlayerAction(whitePos, whitePos.move(x, y)), IllegalMove, newBoard1))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(4, 4), King(WHITE_PLAYER, true), None)))
    val blackPos = Position(7,7)
    val testingMoves2 = List((-2,-1),(-2,1),(-1,-6),(1,-3),(1,-2),(-1,-3),(-1,-2))
    testingMoves2.foreach((x, y) => testMoveError(BLACK_PLAYER, PlayerAction(blackPos, blackPos.move(x, y)), IllegalMove, newBoard2))
  }

  test("A king should capture like a unit star") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(5, 2) -> Pawn(BLACK_PLAYER, true))
      + (Position(5, 7) -> Pawn(WHITE_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(5,1)
    testMove(WHITE_PLAYER, PlayerAction(whitePos, Position(5, 2)), King(WHITE_PLAYER, true), newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(6, 1), Position(5, 1), King(WHITE_PLAYER, true), None)))
    val blackPos = Position(5,8)
    testMove(BLACK_PLAYER, PlayerAction(blackPos, Position(5, 7)), King(BLACK_PLAYER, true), newBoard2)
  }

  test("A king should not capture a piece of its own side") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(5, 2) -> Pawn(WHITE_PLAYER, true))
      + (Position(5, 7) -> Pawn(BLACK_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(5,1)
    testMoveError(WHITE_PLAYER, PlayerAction(whitePos, Position(5, 2)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(6, 1), Position(5, 1), King(WHITE_PLAYER, true), None)))
    val blackPos = Position(5,8)
    testMoveError(BLACK_PLAYER, PlayerAction(blackPos, Position(5, 7)), IllegalMove, newBoard2)
  }

  test("A king should not capture like something else") {
    val emptyBoard = Board.emptyBoard
    val newPieces = emptyBoard.pieces
      + (Position(5, 5) -> Pawn(BLACK_PLAYER, true))
      + (Position(5, 6) -> Pawn(WHITE_PLAYER, true))

    val newBoard1 = Board(newPieces)
    val whitePos = Position(5,1)
    testMoveError(WHITE_PLAYER, PlayerAction(whitePos, Position(5, 5)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(6, 1), Position(5, 1), King(WHITE_PLAYER, true), None)))
    val blackPos = Position(5,8)
    testMoveError(BLACK_PLAYER, PlayerAction(blackPos, Position(5, 6)), IllegalMove, newBoard2)
  }

  test("A king should be able to castle") {
    val defaultBoard = Board.defaultInitialPosition
    val newPieces = defaultBoard.pieces
      - Position(2, 1) - Position(2, 8)
      - Position(3, 1) - Position(3, 8)
      - Position(4, 1) - Position(4, 8)
      - Position(6, 1) - Position(6, 8)
      - Position(7, 1) - Position(7, 8)

    //  a b c d e f g h
    //8 r . . . k . . r
    //7 p p p p p p p p
    //6 . . . . . . . .
    //5 . . . . . . . .
    //4 . . . . . . . .
    //3 . . . . . . . .
    //2 P P P P P P P P
    //1 r . . . K . . r

    val newBoard1 = Board(newPieces)
    val whiteKingSide = testMove(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(7, 1)), King(WHITE_PLAYER, true), newBoard1)
    assert(whiteKingSide.pieces.get(Position(7,1)).contains(King(WHITE_PLAYER, true)))
    assert(whiteKingSide.pieces.get(Position(6,1)).contains(Rook(WHITE_PLAYER, true)))
    val whiteQueenSide = testMove(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(3, 1)), King(WHITE_PLAYER, true), newBoard1)
    assert(whiteQueenSide.pieces.get(Position(3,1)).contains(King(WHITE_PLAYER, true)))
    assert(whiteQueenSide.pieces.get(Position(4,1)).contains(Rook(WHITE_PLAYER, true)))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(4, 1), Position(5, 1), King(WHITE_PLAYER, true), None)))
    val blackKingSide = testMove(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(7, 8)), King(BLACK_PLAYER, true), newBoard2)
    assert(blackKingSide.pieces.get(Position(7,8)).contains(King(BLACK_PLAYER, true)))
    assert(blackKingSide.pieces.get(Position(6,8)).contains(Rook(BLACK_PLAYER, true)))
    val blackQueenSide = testMove(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(3, 8)), King(BLACK_PLAYER, true), newBoard2)
    assert(blackQueenSide.pieces.get(Position(3,8)).contains(King(BLACK_PLAYER, true)))
    assert(blackQueenSide.pieces.get(Position(4,8)).contains(Rook(BLACK_PLAYER, true)))
  }

  test("A king should not be able to castle after moved") {
    val defaultBoard = Board.emptyBoard
    val newPieces = defaultBoard.pieces
      - Position(5, 1) + (Position(5, 1) -> King(WHITE_PLAYER, true))
      - Position(5, 8) + (Position(5, 8) -> King(BLACK_PLAYER, true))
      + (Position(1, 1) -> Rook(WHITE_PLAYER, false))
      + (Position(8, 1) -> Rook(WHITE_PLAYER, false))
      + (Position(1, 8) -> Rook(BLACK_PLAYER, false))
      + (Position(8, 8) -> Rook(BLACK_PLAYER, false))

    //  a b c d e f g h
    //8 r . . . k . . r
    //7 p p p p p p p p
    //6 . . . . . . . .
    //5 . . . . . . . .
    //4 . . . . . . . .
    //3 . . . . . . . .
    //2 P P P P P P P P
    //1 r . . . K . . r

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(7, 1)), IllegalMove, newBoard1)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(3, 1)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(5, 1), King(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(7, 8)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(3, 8)), IllegalMove, newBoard2)
  }

  test("A king should not be able to castle if the rook is moved") {
    val defaultBoard = Board.emptyBoard
    val newPieces = defaultBoard.pieces
      - Position(5, 1) + (Position(5, 1) -> King(WHITE_PLAYER, false))
      - Position(5, 8) + (Position(5, 8) -> King(BLACK_PLAYER, false))
      + (Position(1, 1) -> Rook(WHITE_PLAYER, false))
      + (Position(8, 1) -> Rook(WHITE_PLAYER, true))
      + (Position(1, 8) -> Rook(BLACK_PLAYER, true))
      + (Position(8, 8) -> Rook(BLACK_PLAYER, false))

    //  a b c d e f g h
    //8 r . . . k . . r
    //7 p p p p p p p p
    //6 . . . . . . . .
    //5 . . . . . . . .
    //4 . . . . . . . .
    //3 . . . . . . . .
    //2 P P P P P P P P
    //1 r . . . K . . r

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(7, 1)), IllegalMove, newBoard1)
    val whiteQueenSide = testMove(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(3, 1)), King(WHITE_PLAYER, true), newBoard1)
    assert(whiteQueenSide.pieces.get(Position(3,1)).contains(King(WHITE_PLAYER, true)))
    assert(whiteQueenSide.pieces.get(Position(4,1)).contains(Rook(WHITE_PLAYER, true)))

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(5, 1), King(WHITE_PLAYER, true), None)))
    val blackKingSide = testMove(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(7, 8)), King(BLACK_PLAYER, true), newBoard2)
    assert(blackKingSide.pieces.get(Position(7,8)).contains(King(BLACK_PLAYER, true)))
    assert(blackKingSide.pieces.get(Position(6,8)).contains(Rook(BLACK_PLAYER, true)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(3, 8)), IllegalMove, newBoard2)
  }

  test("A king should not be able to castle when checked") {
    val defaultBoard = Board.emptyBoard
    val newPieces = defaultBoard.pieces
      + (Position(1, 1) -> Rook(WHITE_PLAYER, false))
      + (Position(8, 1) -> Rook(WHITE_PLAYER, false))
      + (Position(1, 8) -> Rook(BLACK_PLAYER, false))
      + (Position(8, 8) -> Rook(BLACK_PLAYER, false))
      + (Position(5, 4) -> Rook(BLACK_PLAYER, true))
      + (Position(5, 5) -> Rook(WHITE_PLAYER, true))

    //  a b c d e f g h
    //8 r . . . k . . r
    //7 . . . . . . . .
    //6 . . . . . . . .
    //5 . . . . R . . .
    //4 . . . . r . . .
    //3 . . . . . . . .
    //2 . . . . . . . .
    //1 R . . . K . . R

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(7, 1)), IllegalMove, newBoard1)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(3, 1)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(5, 1), King(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(7, 8)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(3, 8)), IllegalMove, newBoard2)
  }

  test("A king should not be able to castle if its path is under attack") {
    val defaultBoard = Board.emptyBoard
    val newPieces = defaultBoard.pieces
      + (Position(1, 1) -> Rook(WHITE_PLAYER, false))
      + (Position(8, 1) -> Rook(WHITE_PLAYER, false))
      + (Position(1, 8) -> Rook(BLACK_PLAYER, false))
      + (Position(8, 8) -> Rook(BLACK_PLAYER, false))
      + (Position(6, 4) -> Rook(BLACK_PLAYER, true))
      + (Position(4, 4) -> Rook(BLACK_PLAYER, true))
      + (Position(6, 5) -> Rook(WHITE_PLAYER, true))
      + (Position(4, 5) -> Rook(WHITE_PLAYER, true))

    //  a b c d e f g h
    //8 r . . . k . . r
    //7 . . . . . . . .
    //6 . . . . . . . .
    //5 . . . R . R . .
    //4 . . . r . r . .
    //3 . . . . . . . .
    //2 . . . . . . . .
    //1 R . . . K . . R

    val newBoard1 = Board(newPieces)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(7, 1)), IllegalMove, newBoard1)
    testMoveError(WHITE_PLAYER, PlayerAction(Position(5, 1), Position(3, 1)), IllegalMove, newBoard1)

    val newBoard2 = Board(newPieces, Some(Move(WHITE_PLAYER, Position(1, 1), Position(5, 1), King(WHITE_PLAYER, true), None)))
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(7, 8)), IllegalMove, newBoard2)
    testMoveError(BLACK_PLAYER, PlayerAction(Position(5, 8), Position(3, 8)), IllegalMove, newBoard2)
  }