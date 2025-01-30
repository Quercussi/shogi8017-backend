package com.shogi8017.app.services.logics

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import com.shogi8017.app.errors.*
import com.shogi8017.app.services.*
import com.shogi8017.app.services.logics.Board.*
import com.shogi8017.app.services.logics.BoardAction.{ADD, REMOVE}
import com.shogi8017.app.services.logics.NonPromotablePieceType.*
import com.shogi8017.app.services.logics.PromotablePieceType.*

import scala.annotation.tailrec

sealed trait PieceType

enum PromotablePieceType extends PieceType:
  case QUEEN, ROOK, BISHOP, KNIGHT

enum NonPromotablePieceType extends PieceType:
  case PAWN, KING

sealed trait Piece {
  val owner: Player
  val hasMoved: Boolean

  def pieceType: PieceType

  def withMoved: Piece

  def hasLegalMoves(board: Board, from: Position): Boolean

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition]

  def getAllPossibleMoves(board: Board, position: Position): Set[Position]
  
  private def isSelfPin(board: Board, move: PlayerAction): Boolean = {
    val (from, to) = move.getFromToPositions
    
    val tempBoard = board.copy(
      pieces = board.pieces - from + (to -> board.pieces(from))
    )

    isChecked(tempBoard, this.owner)
  }

  protected def getMoveDeltas(move: PlayerAction): (Position, Position, Int, Int) = {
    val (from, to) = move.getFromToPositions
    val dx = Math.abs(to.x - from.x)
    val dy = Math.abs(to.y - from.y)
    (from, to, dx, dy)
  }

  protected def isPathClear(board: Board, move: PlayerAction, canCaptureDestination: Boolean = true): Boolean = {
    val (from, to) = move.getFromToPositions
    val dx = to.x - from.x
    val dy = to.y - from.y

    // This unit of code works for straight line and diagonal lines.
    val steps = Math.max(Math.abs(dx), Math.abs(dy))
    val xDir = if (dx == 0) 0 else dx / Math.abs(dx)
    val yDir = if (dy == 0) 0 else dy / Math.abs(dy)

    // Check for intermediate positions excluding the destination
    val isPathClear = (1 until steps).forall { step =>
      val intermediatePosition = Position(from.x + step * xDir, from.y + step * yDir)
      !board.pieces.contains(intermediatePosition)
    }

    // For the destination position, ensure it's either empty or an opponent's piece
    val destinationPiece = board.pieces.get(to)
    val isDestinationOccupiedBySelf = destinationPiece.exists(_.owner == this.owner)
    val isDestinationValid = if canCaptureDestination then
      !destinationPiece.exists(_.owner == this.owner)
    else
      destinationPiece.isEmpty

    isPathClear && isDestinationValid
  }

  protected def canOccupyDirectionList(board: Board, position: Position, directions: List[(Int, Int)]): Boolean = {
    directions.exists { direction =>
      !position.isOutOfBoard && canOccupy(board, position, direction)
    }
  }

  protected def canOccupy(board: Board, position: Position, direction: (Int, Int)): Boolean = {
    val (dx, dy) = direction
    val destination = position.move(dx, dy)
    !destination.isOutOfBoard && board.pieces.get(destination).forall(_.owner != this.owner)
  }

  private def isCapture(board: Board, position: Position, direction: (Int, Int)): Boolean = {
    val (dx, dy) = direction
    val destination = position.move(dx, dy)
    board.pieces.get(destination).exists(_.owner != owner)
  }

  protected def getAllPossibleMovesInDirections(board: Board, position: Position, directions: List[(Int, Int)]): Set[Position] = {
    @tailrec
    def occupyDirection(position: Position, direction: (Int, Int), acc: Set[Position] = Set.empty): Set[Position] = {
      val (dx, dy) = direction
      val dest = position.move(dx, dy)
      (canOccupy(board, position, direction), isCapture(board, position, direction)) match {
        case (true, false) => occupyDirection(dest, direction, acc + dest)
        case (true, true) => acc + dest
        case _ => acc
      }
    }

    directions.flatMap(direction => occupyDirection(position, direction)).toSet
  }

  protected def getAllUnitMovesInDirection(board: Board, position: Position, directions: List[(Int, Int)]): Set[Position] = {
    directions.foldLeft(Set.empty[Position]) { (acc, direction) =>
      val dest = position.move(direction._1, direction._2)
      if (canOccupy(board, position, direction)) acc + dest else acc
    }
  }
}

object Piece {
  def validateAndApplyMove(piece: Piece, board: Board, move: PlayerAction): Validated[MoveValidationError, BoardStateTransition] = {
    if (!piece.isSelfPin(board, move)) {
      Invalid(IllegalMove)
    }

    val boardTransitionValidated = piece.getBoardTransition(board, move)

    boardTransitionValidated.andThen((stateTransition, algebraicNotation) => {
      val newPieces = processAction(board.pieces)(stateTransition)
      val newMove = getPlayerActionAsMove(piece.owner, piece, move)

      val newBoard = Board(newPieces, Some(newMove))
      if isChecked(newBoard, piece.owner) then
        Invalid(IllegalMove)
      else
        Valid((newBoard, stateTransition, algebraicNotation))
    })
  }
}

sealed trait SimpleCapturingPiece extends Piece {
  protected def getActionList(board: Board, move: PlayerAction): BoardTransition = {
    val (from, to) = move.getFromToPositions
    val capturingPiece = board.pieces.get(to)
    val capturingAction: Option[StateTransition] = capturingPiece.map(p => {
      (REMOVE, to, p.owner, p.pieceType)
    })

    val fromAction: Option[StateTransition] = Some((REMOVE, from, owner, pieceType))
    val toAction: Option[StateTransition] = Some((ADD, to, owner, pieceType))

    val actions: List[Option[StateTransition]] = List(capturingAction, fromAction, toAction)
    // TODO: implement algebraic notation
    val filteredActions: BoardTransition = (actions.flatten, "")

    filteredActions
  }
}

sealed trait PromotablePiece extends Piece

sealed trait CrossMovingPiece extends Piece with SimpleCapturingPiece{
  private val directions = List((1, 0), (-1, 0), (0, 1), (0, -1))

  private def hasCrossMove(board: Board, position: Position): Boolean = {
    canOccupyDirectionList(board, position, directions)
  }

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    hasCrossMove(board, from)
  }

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    getAllPossibleMovesInDirections(board, position, directions)
  }
}

sealed trait DiagonalMovingPiece extends Piece with SimpleCapturingPiece{
  private val directions = List((1, 1), (-1, 1), (1, -1), (-1, -1))

  private def hasDiagonalMove(board: Board, position: Position): Boolean = {
    canOccupyDirectionList(board, position, directions)
  }

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    hasDiagonalMove(board, from)
  }

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    getAllPossibleMovesInDirections(board, position, directions)
  }
}

trait StarMovingPiece extends Piece {
  protected val directions: List[(Int, Int)] = List((1, 1), (-1, 1), (1, -1), (-1, -1), (1, 0), (-1, 0), (0, 1), (0, -1))
}

sealed trait QueenlyMovingPiece extends StarMovingPiece with SimpleCapturingPiece {
  private def hasStarMove(board: Board, position: Position): Boolean = {
    canOccupyDirectionList(board, position, directions)
  }

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    hasStarMove(board, from)
  }

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    getAllPossibleMovesInDirections(board, position, directions)
  }
}

sealed trait KinglyMovingPiece extends StarMovingPiece {
  private def positionIfSatisfies(position: Position, predicate: Position => Boolean): Set[Position] =
    Option(position).filter(predicate).toSet

  private def getAllCastlingMoves(board: Board, position: Position): Set[Position] = {
    if (this.hasMoved) return Set.empty

    val castlingOffsets = List(2, -2)
    val castlingRow = this.owner match {
      case Player.WHITE_PLAYER => 1
      case Player.BLACK_PLAYER => 8
    }

    val isValidCastlingPosition = position == Position(5, castlingRow)
    if isValidCastlingPosition then
      castlingOffsets.flatMap { offset =>
        positionIfSatisfies(position, pos => getCastlingStateTransition(board, PlayerAction(pos, pos.move(offset, 0)))._1.nonEmpty)
      }.toSet
    else
      Set.empty
  }

  private def hasCastlingMove(board: Board, position: Position): Boolean = {
    getAllCastlingMoves(board, position).nonEmpty
  }

  override protected def canOccupyDirectionList(board: Board, position: Position, directions: List[(Int, Int)]): Boolean = {
    directions.exists { direction => {
      val dest = position.move(direction._1, direction._2)
      !dest.isOutOfBoard && canOccupy(board, position, direction) && !dest.isUnderAttack(board, owner)
    }}
  }

  private def hasKinglyMove(board: Board, position: Position): Boolean = {
    canOccupyDirectionList(board, position, directions) || hasCastlingMove(board, position)
  }

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    hasKinglyMove(board, from)
  }

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    getAllUnitMovesInDirection(board, position, directions) union getAllCastlingMoves(board, position)
  }

  protected def getCastlingStateTransition(board: Board, move: PlayerAction): BoardTransition = {
    if(this.hasMoved) return (List.empty, "")

    val (from, to) = move.getFromToPositions
    val (dx, dy) = (to.x - from.x, to.y - from.y)


    if (Math.abs(dx) != 2 || dy != 0) return (List.empty, "")
    if (from.isUnderAttack(board, owner)) return (List.empty, "")

    def canCastle(rookFromOffset: Int, kingPath: List[Int]): Boolean = {
      val rookPosition = from.move(rookFromOffset, 0)

      val pathPositions = kingPath.map(from.move(_, 0))
      val isPathClear = pathPositions.forall(pos =>
        !board.pieces.contains(pos) && !pos.isUnderAttack(board, owner)
      )

      board.pieces.get(rookPosition).exists {
        case rook: Rook => !rook.hasMoved && !this.hasMoved && isPathClear
        case _ => false
      }
    }

    def createStateChange(rookFromOffset: Int, rookToOffset: Int): StateTransitionList = {
      List(
        (REMOVE, from, owner, KING),
        (ADD, to, owner, KING),
        (REMOVE, from.move(rookFromOffset, 0), owner, ROOK),
        (ADD, to.move(rookToOffset, 0), owner, ROOK)
      )
    }

    if (dx > 0 && canCastle(3, List(1, 2))) // King-side castling
      (createStateChange(3, -1), "O-O")
    else if (dx < 0 && canCastle(-4, List(-1, -2))) // Queen-side castling
      (createStateChange(-4, 1), "O-O-O")
    else
      (List.empty, "")
  }
}

sealed trait KnightMovingPiece extends Piece with SimpleCapturingPiece {
  private val directions = List((2, 1), (1, 2), (-2, 1), (-1, 2), (2, -1), (1, -2), (-2, -1), (-1, -2))

  private def hasKnightMove(board: Board, position: Position): Boolean = {
    canOccupyDirectionList(board, position, directions)
  }

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    hasKnightMove(board, from)
  }

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    getAllUnitMovesInDirection(board, position, directions)
  }
}

sealed trait PawnlyMovingPiece extends Piece with SimpleCapturingPiece {
  protected val direction: Int

  protected def getForwardMove(board: Board, position: Position): Set[Position] = {
    val forward = position.move(0, direction)
    if (board.pieces.contains(forward)) Set.empty
    else Set(forward)
  }

  protected def getDoubleMove(board: Board, position: Position): Set[Position] = {
    val forward = position.move(0, direction)
    val doubleForward = position.move(0, 2 * direction)
    if (board.pieces.contains(forward) || board.pieces.contains(doubleForward)) Set.empty
    else Set(doubleForward)
  }

  protected def getCaptureMoves(board: Board, position: Position): Set[Position] = {
    val left = position.move(-1, direction)
    val right = position.move(1, direction)
    val leftCapture = board.pieces.get(left).filter(_.owner != owner).map(_ => left)
    val rightCapture = board.pieces.get(right).filter(_.owner != owner).map(_ => right)
    Set(leftCapture, rightCapture).flatten
  }

  // TODO: this is still incorrect
  protected def getEnPassantCapture(board: Board, position: Position): Set[Position] = {
    val left = position.move(-1, direction)
    val right = position.move(1, direction)
    val leftCapture = board.pieces.get(left).filter(_.owner != owner).map(_ => left)
    val rightCapture = board.pieces.get(right).filter(_.owner != owner).map(_ => right)
    Set(leftCapture, rightCapture).flatten
  }

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    val direction = if (owner == Player.WHITE_PLAYER) 1 else -1
    List((0, direction), (0, 2 * direction), (1, direction), (-1, direction)).exists { case (dx, dy) =>
      getBoardTransition(board, PlayerAction(from, from.move(dx, dy))).exists(_._1.nonEmpty)
    }
  }

  def getAllPossibleMoves(board: Board, position: Position): Set[Position] = {
    val forwardMove = getForwardMove(board, position)
    val doubleMove = getDoubleMove(board, position)
    val captureMoves = getCaptureMoves(board, position)
    val enPassantCapture = getEnPassantCapture(board, position)
    forwardMove union doubleMove union captureMoves union enPassantCapture
  }
}

case class King(owner: Player, hasMoved: Boolean = false) extends Piece with KinglyMovingPiece with SimpleCapturingPiece {

  def pieceType: PieceType = KING

  def withMoved: King = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, absDx, absDy) = getMoveDeltas(move)

    val isRegularDirection = (absDx <= 1 && absDy <= 1) && (absDx != 0 || absDy != 0) && isPathClear(board, move, true)
    val castlingChanges = this.getCastlingStateTransition(board, move)

    if (isRegularDirection) {
      val tempBoard = board.copy(pieces = board.pieces - from + (to -> board.pieces(from)))
      Valid(getActionList(board, move))
    }
    else if (castlingChanges._1.nonEmpty)
      Valid(castlingChanges)
    else
      Invalid(IllegalMove)
  }
}

case class Queen(owner: Player, hasMoved: Boolean = false) extends PromotablePiece with QueenlyMovingPiece {

  def pieceType: PieceType = QUEEN

  def withMoved: Queen = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, absDx, absDy) = getMoveDeltas(move)
    val isValidDestination = (absDx == absDy || from.x == to.x || from.y == to.y) && isPathClear(board, move, true)

    if (isValidDestination)
      Valid(getActionList(board, move))
    else
      Invalid(IllegalMove)
  }
}

case class Rook(owner: Player, hasMoved: Boolean = false) extends PromotablePiece with CrossMovingPiece {

  def pieceType: PieceType = ROOK

  def withMoved: Rook = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to) = move.getFromToPositions
    val isValidDestination = (from.x == to.x || from.y == to.y) && isPathClear(board, move, true)

    if (isValidDestination)
      Valid(getActionList(board, move))
    else
      Invalid(IllegalMove)
  }
}

case class Bishop(owner: Player, hasMoved: Boolean = false) extends PromotablePiece with DiagonalMovingPiece {

  def pieceType: PieceType = BISHOP

  def withMoved: Bishop = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, absDx, absDy) = getMoveDeltas(move)
    
    val isValidDestination = absDx == absDy && isPathClear(board, move, true)

    if (isValidDestination)
      Valid(getActionList(board, move))
    else
      Invalid(IllegalMove)
  }
}

case class Knight(owner: Player, hasMoved: Boolean = false) extends PromotablePiece with KnightMovingPiece {

  def pieceType: PieceType = KNIGHT

  def withMoved: Knight = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, absDx, absDy) = getMoveDeltas(move)
    val isValidDestination = (absDx == 2 && absDy == 1 || absDx == 1 && absDy == 2) && board.pieces.get(to).forall(_.owner != owner)

    if (isValidDestination)
      Valid(getActionList(board, move))
    else
      Invalid(IllegalMove)
  }
}

case class Pawn(owner: Player, hasMoved: Boolean = false) extends Piece with PawnlyMovingPiece {
  val direction: Int = if (owner == Player.WHITE_PLAYER) 1 else -1

  def pieceType: PieceType = PAWN

  def withMoved: Pawn = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, promoteTo) = move.getFields
    
    val isOwnerWhite = owner == Player.WHITE_PLAYER
    val opponent = if (isOwnerWhite) Player.BLACK_PLAYER else Player.WHITE_PLAYER

    val direction = if (isOwnerWhite) 1 else -1
    val dx = to.x - from.x
    val dy = to.y - from.y

    val destinationPiece = board.pieces.get(to)
    val isDestinationOccupied = destinationPiece.isDefined
    val isDestinationOccupiedByOpponent = destinationPiece.exists(_.owner != this.owner)

    val isRegularDirection = dx == 0 && dy == direction
    val isDoubleDirection = dx == 0 && dy == 2 * direction
    val isCapturingDirection = Math.abs(dx) == 1 && dy == direction

    def isStepMove: Boolean =           isRegularDirection   && isPathClear(board, move, false)
    def isDoubleMove: Boolean =         isDoubleDirection    && isPathClear(board, move, false)
    def isRegularCaptureMove: Boolean = isCapturingDirection && isDestinationOccupiedByOpponent
    def enPassantStateTransition: BoardTransition = getEnPassantStateTransition(board, move)

    val lastRow = if (isOwnerWhite) 8 else 1
    val effectingPieceAction: Validated[MoveValidationError, StateTransitionList] = (promoteTo, to) match {
      case (None, Position(_, row)) if row == lastRow =>
        Invalid(NoPromotion)
      case (Some(promotablePieceType), Position(_, row)) if row == lastRow =>
        Valid(List(
          (REMOVE, from, owner, PAWN),
          (ADD, to, owner, promotablePieceType)
        ))
      case _ =>
        Valid(List(
          (REMOVE, from, owner, PAWN),
          (ADD, to, owner, PAWN)
        ))
    }
      
    effectingPieceAction.andThen(actionList =>
      if (isStepMove) {
        Valid((actionList, ""))
      } else if (isDoubleMove) {
        if (this.hasMoved)
          Invalid(IllegalMove)
        else
          Valid((actionList, ""))
      } else if (isRegularCaptureMove) {
        destinationPiece match {
          case Some(capturingPiece) => Valid(((REMOVE, to, opponent, capturingPiece.pieceType)::actionList, ""))
          case None => Invalid(IllegalMove)
        }
      } else if (enPassantStateTransition._1.nonEmpty) {
        Valid(enPassantStateTransition)
      } else {
        Invalid(IllegalMove)
      }
    )
  }

  private def getEnPassantStateTransition(board: Board, move: PlayerAction): BoardTransition = {
    val (from, to) = move.getFromToPositions
    
    board.lastMove match {
      case Some(Move(lastPlayer, lastFrom, lastTo, lastPiece: Pawn, _)) =>
        val isOwnerWhite = owner == Player.WHITE_PLAYER
        val direction = if (isOwnerWhite) 1 else -1
        val enPassantRow = if (isOwnerWhite) 5 else 4

        val isCorrectRowFrom = from.y == enPassantRow
        val isCorrectRowTo = to.y - from.y == direction
        val isCorrectColumnFrom = Math.abs(from.x - lastTo.x) == 1
        val isCorrectColumnTo = to.x == lastTo.x
        val isOpponentPawn = lastPiece.owner != owner

        if(isCorrectRowFrom && isCorrectRowTo && isCorrectColumnFrom && isCorrectColumnTo && isOpponentPawn) {
          (List(
            (REMOVE, lastTo, lastPlayer, PAWN),
            (REMOVE, from, owner, PAWN),
            (ADD, to, owner, PAWN))
          , "")
        } else {
          (List.empty, "")
        }

      case _ => (List.empty, "")
    }
  }
}