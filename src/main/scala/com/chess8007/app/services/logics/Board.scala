package com.shogi8017.app.services.logics

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all.*
import com.shogi8017.app.errors.*
import GameEvent.{CHECK, CHECKMATE, DEAD_POSITION, STALEMATE}
import com.shogi8017.app.services.*
import com.shogi8017.app.services.logics.NonPromotablePieceType.*
import com.shogi8017.app.services.logics.Piece.validateAndApplyMove
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.PromotablePieceType.*

type MoveResult = (Board, StateTransitionList, AlgebraicNotation, Option[GameEvent])
type BoardStateTransition = (Board, StateTransitionList, AlgebraicNotation)
type BoardTransition = (StateTransitionList, AlgebraicNotation)
type StateTransitionList = List[StateTransition]
type StateTransition = (BoardAction, Position, Player, PieceType)
type AlgebraicNotation = String
  
enum BoardAction: 
  case REMOVE, ADD

case class Board(pieces: Map[Position, Piece], lastMove: Option[Move] = None) {}

object Board {
  def defaultInitialPosition: Board = {
    def createPawns(player: Player, row: Int): Map[Position, Piece] =
      (1 to 8).map(col => Position(col, row) -> Pawn(player)).toMap

    def createMajorPieces(player: Player, row: Int): Map[Position, Piece] = {
      val pieceOrder = Seq(
        Rook(player), Knight(player), Bishop(player),
        Queen(player), King(player),
        Bishop(player), Knight(player), Rook(player)
      )
      pieceOrder.zipWithIndex.map { case (piece, col) =>
        Position(col + 1, row) -> piece
      }.toMap
    }

    val whitePieces = createMajorPieces(Player.WHITE_PLAYER, 1) ++ createPawns(Player.WHITE_PLAYER, 2)
    val blackPieces = createMajorPieces(Player.BLACK_PLAYER, 8) ++ createPawns(Player.BLACK_PLAYER, 7)

    Board(whitePieces ++ blackPieces)
  }

  def emptyBoard: Board = Board(
    Map(Position(5,1) -> King(WHITE_PLAYER), Position(5,8) -> King(BLACK_PLAYER)),
    None
  )

  def fromMovesList(moveList: List[(Player, PlayerAction)]): Validated[GameValidationError, Board] = {
    moveList.foldLeft(Valid(Board.defaultInitialPosition): Validated[GameValidationError, Board]) { (validatedBoard, move) =>
      validatedBoard.andThen { board =>
        val (player, playerAction) = move
        executeMove(board, player, playerAction).map {
          case (newBoard, _, _, _) => newBoard
        }
      }
    }
  }

  def executeMove(board: Board, player: Player, playerAction: PlayerAction): Validated[GameValidationError, MoveResult] = {

    def validateGameState(player: Player): Validated[GameValidationError, Unit] = {
      Validated.cond(getKingPosition(board, player).nonEmpty, (), NoKingError)
    }

    def validatePlayerAction(player: Player, playerAction: PlayerAction): Validated[MoveValidationError, Unit] = {
      val (from, to) = playerAction.getFromToPositions

      val errors = List(
        if (isOutOfTurn(board.lastMove, player)) Some(OutOfTurn) else None,
        if (from == to) Some(NoMove) else None,
        if (to.isOutOfBoard) Some(OutOfBoard) else None
      ).flatten

      errors.headOption match {
        case Some(error) => Invalid(error)
        case None => Valid(())
      }
    }

    def validatePieceExistence(from: Position): Validated[MoveValidationError, Piece] = {
      Validated.cond(board.pieces.contains(from), board.pieces(from), UnoccupiedPosition)
    }

    def processMove(board: Board, player: Player, playerAction: PlayerAction)(piece: Piece): (Piece, Validated[GameValidationError, BoardStateTransition]) = {
      (piece, validateAndApplyMove(piece, board, playerAction))
    }

    def processGameEvent(inputTuple: (Piece, Validated[GameValidationError, BoardStateTransition])): Validated[GameValidationError, MoveResult] = {
      val (movingPiece, moveExecution) = inputTuple
      moveExecution.andThen { (board, stateTransitionList, algebraicNotation) =>
        val gameEvent = checkGameEvent(board, movingPiece.owner)

        val newAlgebraicNotation = gameEvent match
          case Some(CHECK) | Some(CHECKMATE) => algebraicNotation + (if gameEvent.contains(CHECK) then " +" else " #")
          case Some(STALEMATE) | Some(DEAD_POSITION) => algebraicNotation + " 1/2-1/2"
          case None => algebraicNotation

        Valid((board, stateTransitionList, algebraicNotation, gameEvent))
      }
    }

    def checkGameEvent(board: Board, player: Player): Option[GameEvent] = {
      lazy val opponent = if (player == WHITE_PLAYER) BLACK_PLAYER else WHITE_PLAYER
      lazy val checked = isChecked(board, opponent)
      lazy val escape = hasEscape(board, opponent)
      lazy val stalemate = isStalemate(board, opponent)
      lazy val deadPosition = isDeadPosition(board)

      (checked, escape, stalemate, deadPosition) match {
        case (true, true, _, _) => Some(CHECK)
        case (true, false, _, _) => Some(CHECKMATE)
        case (_, _, true, _) => Some(STALEMATE)
        case (_, _, _, true) => Some(DEAD_POSITION)
        case _ => None
      }
    }

    val (from, to) = playerAction.getFromToPositions
    validateGameState(player)
      .andThen(_ => validatePlayerAction(player, playerAction))
      .andThen(_ => validatePieceExistence(from))
      .andThen(processGameEvent compose processMove(board, player, playerAction))
  }

  private def isOutOfTurn(lastMove: Option[Move], currentPlayer: Player): Boolean =
    lastMove match
      case None => currentPlayer == BLACK_PLAYER
      case Some(lastMove) => lastMove.player == currentPlayer

  private def isStalemate(board: Board, player: Player): Boolean = {
    !isChecked(board, player) && !forallPlayerPieces(board, player) { (position, piece) =>
      piece.hasLegalMoves(board, position)
    }
  }

  private def isDeadPosition(board: Board): Boolean = {
    val pieces = board.pieces.values.toList

    // Case 1: King vs. King
    def isKingVsKing: Boolean = pieces.forall(_.isInstanceOf[King])

    // Case 2: King and bishop vs. King or King and knight vs. King
    def isKingAndBishopVsKingOrKingAndKnightVsKing: Boolean = {
      pieces.size == 3 && pieces.count(_.isInstanceOf[King]) == 2 &&
        pieces.exists(p => p.isInstanceOf[Knight] || p.isInstanceOf[Bishop])
    }

    // Case 3: King and bishop vs. King and bishop of opposite colors
    def isKingAndBishopVsKingAndBishop: Boolean = {
      pieces.size == 4 && {
        val bishops = board.pieces.collect { case (position: Position, b: Bishop) => (position, b) }
        if (bishops.size != 2) return false

        bishops.headOption.flatMap { case (pos1, b1) =>
          bishops.tail.headOption.map { case (pos2, b2) =>
            b1.owner != b2.owner && pos1.getPositionColor == pos2.getPositionColor
          }
        }.getOrElse(false)
      }
    }

    val (x, y, z) = (isKingVsKing, isKingAndBishopVsKingOrKingAndKnightVsKing, isKingAndBishopVsKingAndBishop)

    isKingVsKing || isKingAndBishopVsKingOrKingAndKnightVsKing || isKingAndBishopVsKingAndBishop
  }

  def processAction(pieceMap: Map[Position, Piece])(stateTransitionList: StateTransitionList): Map[Position, Piece] = {
    stateTransitionList.foldLeft(pieceMap) { (acc, transition) =>
      val (action, position, player, pieceType) = transition
      action match {
        case BoardAction.REMOVE => acc - position
        case BoardAction.ADD =>
          val piece = pieceType match {
            case PAWN => Pawn(player).withMoved
            case ROOK => Rook(player).withMoved
            case BISHOP => Bishop(player).withMoved
            case KNIGHT => Knight(player).withMoved
            case KING => King(player).withMoved
            case QUEEN => Queen(player).withMoved
          }
          acc + (position -> piece)
      }
    }
  }

  def getPlayerActionAsMove(player: Player, piece: Piece, playerAction: PlayerAction): Move = {
    val (from, to, promoteTo) = playerAction.getFields
    Move(player, from, to, piece, promoteTo)
  }

  def isChecked(board: Board, player: Player): Boolean = {
    val kingPosition = getKingPosition(board, player)
    val k = kingPosition.exists(_.isUnderAttack(board, player))
    kingPosition.exists(_.isUnderAttack(board, player))
  }

  private def hasEscape(board: Board, player: Player): Boolean = {
    existsPlayerPieces(board, player) { (position, piece) =>
      piece.getAllPossibleMoves(board, position).exists { to =>
        val tempBoard = board.copy(
          pieces = board.pieces - position + (to -> piece),
          lastMove = Some(Move(player, position, to, piece))
        )

        // The invalidity of the `isCheck` method can be disregarded here, as it will only be invalid if there is no king.
        // If there is no king, this function will not be invoked in the first place.
        val k = !isChecked(tempBoard, player)
        !isChecked(tempBoard, player)
      }
    }
  }

  private def isCheckmated(board: Board, player: Player): Boolean = {

    isChecked(board, player) && !hasEscape(board, player)
  }

  def existsPlayerPieces(board: Board, player: Player)(f: (Position, Piece) => Boolean): Boolean = {
    board.pieces.filter {
      case (position, piece) => piece.owner == player
    }.exists {
      case (position, piece) => f(position, piece)
    }
  }

  private def forallPlayerPieces(board: Board, player: Player)(f: (Position, Piece) => Boolean): Boolean = {
    board.pieces.filter {
      case (position, piece) => piece.owner == player
    }.forall {
      case (position, piece) => f(position, piece)
    }
  }

  private def getKingPosition(board: Board, player: Player): Option[Position] = {
    board.pieces.collectFirst {
      case (position, king: King) if king.owner == player => Some(position)
    }.flatten
  }
}

