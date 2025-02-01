package com.shogi8017.app.services.logics

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all.*
import com.shogi8017.app.errors.*
import com.shogi8017.app.services.*
import com.shogi8017.app.services.logics.GameEvent.{CHECK, CHECKMATE, DEAD_POSITION, STALEMATE}
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER}
import com.shogi8017.app.services.logics.pieces.*
import com.shogi8017.app.services.logics.pieces.Piece.validateAndApplyAction
import com.shogi8017.app.services.logics.pieces.PieceType.getPieceByPieceType
import com.shogi8017.app.services.logics.utils.Multiset

type MoveResult = (Board, StateTransitionList, AlgebraicNotation, Option[GameEvent])
type BoardStateTransition = (Board, StateTransitionList, AlgebraicNotation)
type BoardTransition = (StateTransitionList, AlgebraicNotation)
type StateTransitionList = List[StateTransition]
type StateTransition = (BoardAction, Position, Player, PieceType)
type AlgebraicNotation = String
  
enum BoardAction: 
  case REMOVE, ADD, HAND_ADD, HAND_REMOVE

case class Board(
  piecesMap: Map[Position, Piece],
  hands: Map[Player, Multiset[PieceType]] = Map(Player.WHITE_PLAYER -> Multiset.empty,
          Player.BLACK_PLAYER -> Multiset.empty),
  lastAction: Option[Action] = None
)

object Board {
  def defaultInitialPosition: Board = {
    def createPawns(player: Player, row: Int): Map[Position, Piece] =
      (1 to 9).map(col => Position(col, row) -> Pawn(player)).toMap

    def createMajorPieces(player: Player, row: Int): Map[Position, Piece] = {
      val pieceOrder = Seq(
        Lance(player), Knight(player), Silver(player),
        Gold(player), King(player), Gold(player),
        Silver(player), Knight(player), Lance(player)
      )
      pieceOrder.zipWithIndex.map { case (piece, col) =>
        Position(col + 1, row) -> piece
      }.toMap
    }

    def createSpecialPieces(player: Player, row: Int): Map[Position, Piece] = {
      val (bishopPos, rookPos) = if (player == Player.WHITE_PLAYER) (2, 8) else (8, 2)
      Map(
        Position(bishopPos, row) -> Bishop(player),
        Position(rookPos, row) -> Rook(player)
      )
    }

    val whitePieces = createMajorPieces(Player.WHITE_PLAYER, 1) ++ createPawns(Player.WHITE_PLAYER, 3) ++ createSpecialPieces(Player.WHITE_PLAYER, 2)
    val blackPieces = createMajorPieces(Player.BLACK_PLAYER, 9) ++ createPawns(Player.BLACK_PLAYER, 7) ++ createSpecialPieces(Player.BLACK_PLAYER, 8)

    Board(whitePieces ++ blackPieces)
  }
  
  def emptyBoard: Board = Board(
    Map(Position(5,1) -> King(WHITE_PLAYER), Position(5,8) -> King(BLACK_PLAYER)),
  )

  def fromMovesList(moveList: List[(Player, MoveAction)]): Validated[GameValidationError, Board] = {
    moveList.foldLeft(Valid(Board.defaultInitialPosition): Validated[GameValidationError, Board]) { (validatedBoard, move) =>
      validatedBoard.andThen { board =>
        val (player, playerAction) = move
        executeMove(board, player, playerAction).map {
          case (newBoard, _, _, _) => newBoard
        }
      }
    }
  }

  def executeMove(board: Board, player: Player, playerAction: MoveAction): Validated[GameValidationError, MoveResult] = {

    def validateGameState(player: Player): Validated[GameValidationError, Unit] = {
      Validated.cond(getKingPosition(board, player).nonEmpty, (), NoKingError)
    }

    def validatePlayerAction(player: Player, playerAction: MoveAction): Validated[ActionValidationError, Unit] = {
      val (from, to) = playerAction.getFromToPositions

      val errors = List(
        if (isOutOfTurn(board.lastAction, player)) Some(OutOfTurn) else None,
        if (from == to) Some(NoMove) else None,
        if (to.isOutOfBoard) Some(OutOfBoard) else None
      ).flatten

      errors.headOption match {
        case Some(error) => Invalid(error)
        case None => Valid(())
      }
    }

    def validatePieceExistence(from: Position): Validated[ActionValidationError, Piece] = {
      Validated.cond(isOccupied(board, from), board.piecesMap(from), UnoccupiedPosition)
    }

    def processMove(board: Board, player: Player, playerAction: MoveAction)(piece: Piece): (Piece, Validated[GameValidationError, BoardStateTransition]) = {
      (piece, validateAndApplyAction(piece, board, playerAction))
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

  private def isOutOfTurn(lastAction: Option[Action], currentPlayer: Player): Boolean =
    lastAction match
      case None => currentPlayer == BLACK_PLAYER
      case Some(lastAction) => lastAction.player == currentPlayer

  private def isStalemate(board: Board, player: Player): Boolean = {
    !isChecked(board, player) && !forallPlayerPieces(board, player) { (position, piece) =>
      piece.hasLegalMoves(board, position)
    }
  }

  private def isDeadPosition(board: Board): Boolean = {
    val pieces = board.piecesMap.values.toList

    // Case 1: Lance vs. Lance
    def isKingVsKing: Boolean = pieces.forall(_.isInstanceOf[King])

    // Case 2: Lance and bishop vs. Lance or Lance and knight vs. Lance
    def isKingAndBishopVsKingOrKingAndKnightVsKing: Boolean = {
      pieces.size == 3 && pieces.count(_.isInstanceOf[King]) == 2 &&
        pieces.exists(p => p.isInstanceOf[Knight] || p.isInstanceOf[Bishop])
    }

    isKingVsKing || isKingAndBishopVsKingOrKingAndKnightVsKing
  }

  def processAction(board: Board, actingPlayer: Player)(stateTransitionList: StateTransitionList): Board = {
    val updatedBoard = stateTransitionList.foldLeft(board) { (acc, transition) =>
      val (boardAction, position, player, pieceType) = transition
      boardAction match {
        case BoardAction.REMOVE =>
          acc.copy(piecesMap = acc.piecesMap - position)
        case BoardAction.ADD =>
          val piece = PieceType.getPieceByPieceType(pieceType, player)
          acc.copy(piecesMap = acc.piecesMap + (position -> piece))
        case BoardAction.HAND_ADD =>
          val updatedHand = acc.hands.getOrElse(player, Multiset.empty) + pieceType
          acc.copy(hands = acc.hands + (player -> updatedHand))
        case BoardAction.HAND_REMOVE =>
          val updatedHand = acc.hands.getOrElse(player, Multiset.empty) - pieceType
          acc.copy(hands = acc.hands + (player -> updatedHand))
      }
    }
    updatedBoard.copy(lastAction = Some(Action(actingPlayer)))
  }

  def isChecked(board: Board, player: Player): Boolean = {
    val kingPosition = getKingPosition(board, player)
    val k = kingPosition.exists(_.isUnderAttack(board, player))
    kingPosition.exists(_.isUnderAttack(board, player))
  }

  private def hasEscape(board: Board, player: Player): Boolean = {
    lazy val moveEscape = existsPlayerPieces(board, player) { (position, piece) =>
      piece.getAllPossibleMoves(board, position).exists { to =>
        val updatedHands = board.piecesMap.get(to) match {
          case Some(p) if p.owner != player => board.hands.updated(player, board.hands(player) + p.pieceType)
          case _ => board.hands
        }
        val tempBoard = board.copy(
          piecesMap = board.piecesMap - position + (to -> piece),
          hands = updatedHands,
          lastAction = Some(Action(player))
        )

        !isChecked(tempBoard, player)
      }
    }
    
    lazy val dropEscape = existsPlayerHands(board, player) {
      case piece@(droppablePiece: DroppablePiece) =>
        droppablePiece.getAllPossibleDrops(board).exists { to =>
          val tempBoard = board.copy(
            piecesMap = board.piecesMap + (to -> piece),
            hands = board.hands.updated(player, board.hands(player) - piece.pieceType),
            lastAction = Some(Action(player))
          )

          !isChecked(tempBoard, player)
        }
      case _ => false
    }

    moveEscape || dropEscape
  }

  def isCheckmated(board: Board, player: Player): Boolean = {
    isChecked(board, player) && !hasEscape(board, player)
  }

  def existsPlayerPieces(board: Board, player: Player)(f: (Position, Piece) => Boolean): Boolean = {
    board.piecesMap.filter {
      case (position, piece) => piece.owner == player
    }.exists {
      case (position, piece) => f(position, piece)
    }
  }

  def existsPlayerHands(board: Board, player: Player)(f: Piece => Boolean): Boolean = {
    board.hands.getOrElse(player, Multiset.empty).toSet
      .map(p => getPieceByPieceType(p, player))
      .exists(f)
  }

  private def forallPlayerPieces(board: Board, player: Player)(f: (Position, Piece) => Boolean): Boolean = {
    board.piecesMap.filter {
      case (position, piece) => piece.owner == player
    }.forall {
      case (position, piece) => f(position, piece)
    }
  }

  private def getKingPosition(board: Board, player: Player): Option[Position] = {
    board.piecesMap.collectFirst {
      case (position, king: King) if king.owner == player => Some(position)
    }.flatten
  }

  def getEmptyPositions(board: Board): Set[Position] = {
    val allPositions = for {
      x <- 1 to 9
      y <- 1 to 9
    } yield Position(x, y)

    allPositions.toSet -- board.piecesMap.keySet
  }

  def isOccupied(board: Board, position: Position): Boolean = {
    board.piecesMap.contains(position)
  }

  def isPlayerHandContains(board: Board, player: Player, pieceType: PieceType): Boolean = {
    board.hands(player).contains(pieceType)
  }
}

