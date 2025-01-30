package com.shogi8017.app.services.logics

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import com.shogi8017.app.errors.{GameStateError, IllegalMove, MoveValidationError, NoPromotion, OutOfBoard}
import com.shogi8017.app.services.*
import com.shogi8017.app.services.logics.BoardAction.{ADD, REMOVE}

import javax.print.attribute.standard.Destination
import scala.annotation.tailrec

enum PieceType:
  case KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN

sealed trait Piece() {
  val owner: Player
  val hasMoved: Boolean

  def validateMove(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    if(!isSelfPin(board, move)) {
      Invalid(IllegalMove)
    }
    
    getBoardTransition(board, move)
  }
  
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

    tempBoard.isChecked(this.owner)
  }

  protected def getMoveDeltas(move: PlayerAction): (Position, Position, Int, Int) = {
    val (from, to) = move.getFromToPositions
    val dx = Math.abs(to.x - from.x)
    val dy = Math.abs(to.y - from.y)
    (from, to, dx, dy)
  }
  
  protected def isPathClear(board: Board, move: PlayerAction): Boolean = {
    val (from, to) = move.getFromToPositions
    val dx = to.x - from.x
    val dy = to.y - from.y

    // This unit of code works for straight line and diagonal lines.
    val steps = Math.max(Math.abs(dx), Math.abs(dy))
    val xDir = if (dx == 0) 0 else dx / Math.abs(dx)
    val yDir = if (dy == 0) 0 else dy / Math.abs(dy)

    // Check for intermediate positions excluding the destination
    val isPathClear = (1 until (steps - 1)).forall { step =>
      val intermediatePosition = Position(from.x + step * xDir, from.y + step * yDir)
      !board.pieces.contains(intermediatePosition)
    }

    // For the destination position, ensure it's either empty or an opponent's piece
    val destinationPiece = board.pieces.get(to)
    val isDestinationOccupiedBySelf = destinationPiece.exists(_.owner == this.owner)
    isPathClear && !isDestinationOccupiedBySelf
  }

  protected def canOccupyDirectionList(board: Board, position: Position, directions: List[(Int, Int)]): Boolean = {
    directions.exists { direction =>
      canOccupy(board, position, direction)
    }
  }

  private def canOccupy(board: Board, position: Position, direction: (Int, Int)): Boolean = {
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

sealed trait SimpleCapturingPiece extends Piece {
  protected def getActionList(board: Board, move: PlayerAction): BoardTransition = {
    val (from, to) = move.getFromToPositions
    val capturingPiece = board.pieces.get(to)
    val capturingAction: Option[StateTransition] = capturingPiece.flatMap(p => {
      Some((REMOVE, to, p.owner, p.pieceType))
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
    val (from, to) = move.getFromToPositions
    val (dx, dy) = (to.x - from.x, to.y - from.y)
    
    if (Math.abs(dx) != 2 || dy != 0 || from.isUnderAttack(board, owner)) return (List.empty, "")

    def canCastle(rookOffset: Int, pathOffsets: List[Int]): Boolean = {
      val rookPosition = from.move(rookOffset, 0)

      val pathPositions = pathOffsets.map(from.move(_, 0))
      val isPathClear = pathPositions.forall(pos =>
        !board.pieces.contains(pos) && !pos.isUnderAttack(board, owner)
      )

      board.pieces.get(rookPosition).exists {
        case rook: Rook => !rook.hasMoved && !this.hasMoved && isPathClear
        case _ => false
      }
    }

    def createStateChange(rookOffset: Int, rookTargetOffset: Int): StateTransitionList = {
      List(
        (REMOVE, from, owner, PieceType.KING),
        (ADD, to, owner, PieceType.KING),
        (REMOVE, from.move(rookOffset, 0), owner, PieceType.ROOK),
        (ADD, from.move(rookTargetOffset, 0), owner, PieceType.ROOK)
      )
    }

    if (dx > 0 && canCastle(3, List(1, 2))) // King-side castling
      (createStateChange(3, 1), "O-O")
    else if (dx < 0 && canCastle(-4, List(-1, -2, -3))) // Queen-side castling
      (createStateChange(-4, -1), "O-O-O")
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

case class King(owner: Player, hasMoved: Boolean = false) extends Piece with KinglyMovingPiece with SimpleCapturingPiece {

  def pieceType: PieceType = PieceType.KING

  def withMoved: King = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, absDx, absDy) = getMoveDeltas(move)

    val isRegularDirection = (absDx <= 1 && absDy <= 1) && (absDx != 0 || absDy != 0) && isPathClear(board, move)
    val castlingChanges = this.getCastlingStateTransition(board, move)

    if (isRegularDirection)
      Valid(getActionList(board, move))
    else if (castlingChanges._1.nonEmpty)
      Valid(castlingChanges)
    else
      Invalid(IllegalMove)
  }
}

case class Queen(owner: Player, hasMoved: Boolean = false) extends PromotablePiece with QueenlyMovingPiece {

  def pieceType: PieceType = PieceType.QUEEN

  def withMoved: Queen = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, absDx, absDy) = getMoveDeltas(move)
    val isValidDestination = (absDx == absDy || from.x == to.x || from.y == to.y) && isPathClear(board, move)

    if (isValidDestination)
      Valid(getActionList(board, move))
    else
      Invalid(IllegalMove)
  }
}

case class Rook(owner: Player, hasMoved: Boolean = false) extends PromotablePiece with CrossMovingPiece {

  def pieceType: PieceType = PieceType.ROOK

  def withMoved: Rook = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to) = move.getFromToPositions
    val isValidDestination = (from.x == to.x || from.y == to.y) && isPathClear(board, move)

    if (isValidDestination)
      Valid(getActionList(board, move))
    else
      Invalid(IllegalMove)
  }
}

case class Bishop(owner: Player, hasMoved: Boolean = false) extends PromotablePiece with DiagonalMovingPiece {

  def pieceType: PieceType = PieceType.BISHOP

  def withMoved: Bishop = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, absDx, absDy) = getMoveDeltas(move)
    
    val isValidDestination = absDx == absDy && isPathClear(board, move)

    if (isValidDestination)
      Valid(getActionList(board, move))
    else
      Invalid(IllegalMove)
  }
}

case class Knight(owner: Player, hasMoved: Boolean = false) extends PromotablePiece with KnightMovingPiece {

  def pieceType: PieceType = PieceType.KNIGHT

  def withMoved: Knight = copy(hasMoved = true)

  def getBoardTransition(board: Board, move: PlayerAction): Validated[MoveValidationError, BoardTransition] = {
    val (from, to, absDx, absDy) = getMoveDeltas(move)
    val isValidDestination = absDx == 2 && absDy == 1 || absDx == 1 && absDy == 2

    if (isValidDestination)
      Valid(getActionList(board, move))
    else
      Invalid(IllegalMove)
  }
}

case class Pawn(owner: Player, hasMoved: Boolean = false) extends Piece {

  def pieceType: PieceType = PieceType.PAWN

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

    def isStepMove: Boolean =           isRegularDirection   && isPathClear(board, move)
    def isDoubleMove: Boolean =         isDoubleDirection    && isPathClear(board, move) && !this.hasMoved
    def isRegularCaptureMove: Boolean = isCapturingDirection && !isDestinationOccupiedByOpponent
    def enPassantStateTransition: BoardTransition = getEnPassantStateTransition(board, move)

    val lastRow = if (isOwnerWhite) 8 else 1
    val effectingPieceAction: Validated[MoveValidationError, StateTransitionList] = (promoteTo, to) match {
      case (None, Position(_, lastRow)) => Invalid(NoPromotion)
      case (Some(promotablePiece), Position(_, lastRow)) =>
        Valid(List(
          (REMOVE, from, owner, PieceType.PAWN),
          (ADD, to, owner, promotablePiece.pieceType)
        ))
      case (_, pos) =>
        Valid(List(
          (REMOVE, from, owner, PieceType.PAWN),
          (ADD, to, owner, PieceType.PAWN)
        ))
    }
      
    effectingPieceAction.andThen(actionList =>
      if (isStepMove || isDoubleMove) {
        Valid((actionList, ""))
      } else if (isRegularCaptureMove) {
        destinationPiece match {
          case Some(capturingPiece) => Valid((actionList ++ List((REMOVE, to, opponent, capturingPiece.pieceType)), ""))
          case None => Invalid(IllegalMove)
        }
      } else if (enPassantStateTransition._1.nonEmpty) {
        Valid(enPassantStateTransition)
      } else {
        Invalid(IllegalMove)
      }
    )
  }

  def hasLegalMoves(board: Board, from: Position): Boolean = {
    val direction = if (owner == Player.WHITE_PLAYER) 1 else -1
    List((0, direction), (0, 2 * direction), (1, direction), (-1, direction)).exists { case (dx, dy) =>
      getBoardTransition(board, PlayerAction(from, from.move(dx, dy))).exists(_._1.nonEmpty)
    }
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

        (
          List(
            (REMOVE, lastTo, lastPlayer, PieceType.PAWN),
            (REMOVE, from, owner, PieceType.PAWN),
            (ADD, to, owner, PieceType.PAWN),
          ),
          // TODO: implement algebraic notation
          ""
        )

      case _ => (List.empty, "")
    }
  }
}