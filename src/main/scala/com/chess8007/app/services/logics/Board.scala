package com.chess8007.app.services.logics

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all.*
import com.chess8007.app.errors.*
import GameEvent.{CHECK, CHECKMATE, DEAD_POSITION, STALEMATE}
import com.chess8007.app.services.*
import com.chess8007.app.services.logics.PieceType.{BISHOP, KING, KNIGHT, PAWN, QUEEN, ROOK}

type MoveResult = (Board, StateTransitionList, AlgebraicNotation, Option[GameEvent])
type BoardStateTransition = (Board, StateTransitionList, AlgebraicNotation)
type BoardTransition = (StateTransitionList, AlgebraicNotation)
type StateTransitionList = List[StateTransition]
type StateTransition = (BoardAction, Position, Player, PieceType)
type AlgebraicNotation = String
  
enum BoardAction: 
  case REMOVE, ADD

case class Board(pieces: Map[Position, Piece], lastMove: Option[Move] = None) {
  def executeMove(player: Player, playerAction: PlayerAction): Validated[GameValidationError, MoveResult] = {
    
    def validateGameState(player: Player): Validated[GameValidationError, Unit] = {
      Validated.cond(getKingPosition(player).nonEmpty, (), NoKingError)
    }

    def validateMove(player: Player, playerAction: PlayerAction): Validated[MoveValidationError, Unit] = {
      val (from, to) = playerAction.getFromToPositions
      
      val errors = List(
        if (isOutOfTurn(lastMove, player)) Some(OutOfTurn) else None,
        if (from == to) Some(NoMove) else None,
        if (to.isOutOfBoard) Some(OutOfBoard) else None
      ).flatten

      errors.headOption match {
        case Some(error) => Invalid(error)
        case None => Valid(())
      }
    }

    def validatePieceExistence(from: Position): Validated[MoveValidationError, Piece] = {
      Validated.cond(pieces.contains(from), pieces(from), UnoccupiedPosition)
    }

    def processMove(playerAction: PlayerAction)(piece: Piece): (Piece, Validated[GameValidationError, BoardStateTransition]) = {
      (piece, piece.getBoardTransition(this, playerAction).andThen((stateTransitionList, algebraicNotation) => {
        val newPiecesMap = stateTransitionList.foldLeft(pieces) { (acc, transition) =>
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

        val (from, to, promoteTo) = playerAction.getFields
        val newBoard = copy(
          pieces = newPiecesMap,
          lastMove = Some(Move(player, from, to, piece, promoteTo))
        )

        Valid((newBoard, stateTransitionList, algebraicNotation))
      }))
    }

    def processGameEvent(inputTuple: (Piece, Validated[GameValidationError, BoardStateTransition])): Validated[GameValidationError, MoveResult] = {
      val (movingPiece, moveExecution) = inputTuple
      moveExecution.andThen { (board, stateTransitionList, algebraicNotation) =>
        val gameEvent = checkGameEvent(board, movingPiece.owner)

        val newAlgebraicNotation = gameEvent match
          case Some(CHECK) | Some(CHECKMATE)          => algebraicNotation + (if gameEvent.contains(CHECK) then " +" else " #")
          case Some(STALEMATE) | Some(DEAD_POSITION)  => algebraicNotation + " 1/2-1/2"
          case None => algebraicNotation

        Valid((board, stateTransitionList, algebraicNotation, gameEvent))
      }
    }

    def checkGameEvent(board: Board, player: Player): Option[GameEvent] = {
      if (isChecked(player)) Some(CHECK)
      else if (isCheckmated(board, player)) Some(CHECKMATE)
      else if (isStalemate(board, player)) Some(STALEMATE)
      else if (board.isDeadPosition(board)) Some(DEAD_POSITION)
      else None
    }

    val (from, to) = playerAction.getFromToPositions
    validateGameState(player)
      .andThen(_ => validateMove(player, playerAction))
      .andThen(_ => validatePieceExistence(from))
      .andThen(processGameEvent compose processMove(playerAction))
  }

  private def isOutOfTurn(lastMove: Option[Move], currentPlayer: Player): Boolean =
    lastMove.exists(_.player == currentPlayer)

  private def isStalemate(board: Board, player: Player): Boolean = {
    !isChecked(player) && !forallPlayerPieces(player) { (position, piece) =>
      piece.hasLegalMoves(this, position)
    }
  }

  private def isDeadPosition(board: Board): Boolean = {
    val pieces = board.pieces.values.toList

    // Case 1: King vs. King
    def isKingVsKing: Boolean = pieces.forall(_.isInstanceOf[King])

    // Case 2: King and bishop vs. King or King and knight vs. King
    def isKingAndBishopVsKingOrKingAndKnightVsKing: Boolean = {
      pieces.size == 2 && pieces.exists(_.isInstanceOf[King]) &&
        pieces.exists(p => p.isInstanceOf[Bishop] || p.isInstanceOf[Knight])
    }

    // Case 3: King and bishop vs. King and bishop of opposite colors
    def isKingAndBishopVsKingAndBishop: Boolean = {
      pieces.size == 4 && {
        val bishops = board.pieces.collect { case (position: Position, b: Bishop) => (position, b) }
        if(bishops.size != 2) return false
        
        bishops.headOption.flatMap { case (pos1, b1) =>
          bishops.tail.headOption.map { case (pos2, b2) =>
            b1.owner != b2.owner && pos1.getPositionColor == pos2.getPositionColor
          }
        }.getOrElse(false)
      }
    }

    isKingVsKing || isKingAndBishopVsKingOrKingAndKnightVsKing || isKingAndBishopVsKingAndBishop
  }


  def isChecked(player: Player): Boolean = {
    val kingPosition = getKingPosition(player)
    kingPosition.exists(_.isUnderAttack(this, player))
  }

  private def isCheckmated(board: Board, player: Player): Boolean = {
    def hasEscape(player: Player): Boolean = {
      existsPlayerPieces(player) { (position, piece) =>
        piece.getAllPossibleMoves(this, position).exists { to =>
          val tempBoard = copy(
            pieces = pieces - position + (to -> piece),
            lastMove = Some(Move(player, position, to, piece))
          )

          // The invalidity of the `isCheck` method can be disregarded here, as it will only be invalid if there is no king.
          // If there is no king, this function will not be invoked in the first place.
          tempBoard.isChecked(player)
        }
      }
    }

    isChecked(player) && !hasEscape(player)
  }

  // TODO: check other cases of draw
  
  def existsPlayerPieces(player: Player)(f: (Position, Piece) => Boolean): Boolean = {
    pieces.filter {
      case (position, piece) => piece.owner == player
    }.exists {
      case (position, piece) => f(position, piece)
    }
  }

  private def forallPlayerPieces(player: Player)(f: (Position, Piece) => Boolean): Boolean = {
    pieces.filter {
      case (position, piece) => piece.owner == player
    }.forall {
      case (position, piece) => f(position, piece)
    }
  }

  private def getKingPosition(player: Player): Option[Position] = {
    pieces.collectFirst {
      case (position, king: King) if king.owner == player => Some(position)
    }.flatten
  }

}
