package com.shogi8017.app.services.logics

import cats.data.Validated
import cats.data.Validated.{Invalid, Valid}
import cats.syntax.all.*
import com.shogi8017.app.exceptions.*
import com.shogi8017.app.models.UserModel
import com.shogi8017.app.models.enumerators.GameWinner
import com.shogi8017.app.models.enumerators.GameWinner.{BLACK_WINNER, DRAW, WHITE_WINNER}
import com.shogi8017.app.routes.{BoardConfiguration, PieceHandCount, PlayerList, PositionPiecePair}
import com.shogi8017.app.services.*
import com.shogi8017.app.services.logics.GameEvent.{CHECK, CHECKMATE, IMPASSE, RESIGNATION, STALEMATE}
import com.shogi8017.app.services.logics.Player.{BLACK_PLAYER, WHITE_PLAYER, opponent, toWinner}
import com.shogi8017.app.services.logics.actions.*
import com.shogi8017.app.services.logics.pieces.*
import com.shogi8017.app.services.logics.pieces.Piece.validateAndApplyAction
import com.shogi8017.app.services.logics.pieces.PieceType.getPieceByPieceType
import com.shogi8017.app.utils.Multiset
import com.shogi8017.app.websocketPayloads.{BoardConfigurationEvent}

case class Board(
  piecesMap: Map[Position, Piece],
  hands: Map[Player, Multiset[PieceType]] = Map(
    Player.WHITE_PLAYER -> Multiset.empty,
    Player.BLACK_PLAYER -> Multiset.empty
  ),
  auxiliaryState: BoardAuxiliaryState = BoardAuxiliaryState(None),
  currentPlayerTurn: Player = BLACK_PLAYER
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
    Map(Position(5,1) -> King(WHITE_PLAYER), Position(5,9) -> King(BLACK_PLAYER)),
  )

  def fromExecutionList(moveList: List[ExecutionAction]): Validated[GameValidationException, Board] = {
    moveList.foldLeft(Valid(Board.defaultInitialPosition): Validated[GameValidationException, Board]) { (validatedBoard, move) =>
      validatedBoard.andThen { board =>
        val (player, playerAction) = (move.player, move.playerAction)
        executionAction(board, player, playerAction).map {
          case (newBoard, _, _, _) => newBoard
        }
      }
    }
  }

  def executionAction(board: Board, player: Player, playerAction: PlayerAction): Validated[GameValidationException, MoveResult] = {
    playerAction match
      case onBoardAction: OnBoardAction => executeOnBoardAction(board, player, onBoardAction)
      case resignAction: ResignAction => executeResignAction(board, player, resignAction)
  }

  def executeOnBoardAction(board: Board, player: Player, onBoardAction: OnBoardAction): Validated[GameValidationException, MoveResult] = {

    def validateGameState(player: Player): Validated[GameValidationException, Unit] = {
      Validated.cond(getKingPosition(board, player).nonEmpty, (), NoKingException)
        .andThen(_ => Validated.cond(board.auxiliaryState.gameWinner.isEmpty, (), GameAlreadyEndedException))
    }

    def validateOnBoardAction(player: Player, onBoardAction: OnBoardAction): Validated[ActionValidationException, Unit] = {

      val errors: List[ActionValidationException] = {
        val commonErrors = List(
          Option.when(isOutOfTurn(board.currentPlayerTurn, player))(OutOfTurn)
        )

        val specificErrors = onBoardAction match {
          case moveAction: MoveAction =>
            val (from, to) = moveAction.getFromToPositions
            List(
              Option.when(from == to)(NoMove),
              Option.when(to.isOutOfBoard)(OutOfBoard)
            )

          case dropAction: DropAction =>
            List(Option.when(dropAction.position.isOutOfBoard)(OutOfBoard))

          case _ => List(Some(UnknownAction))
        }

        (commonErrors ++ specificErrors).flatten
      }

      errors.headOption match {
        case Some(error) => Invalid(error)
        case None => Valid(())
      }
    }

    def validatePieceExistenceAndOwnership(player: Player, onBoardAction: OnBoardAction): Validated[ActionValidationException, Piece] = {
      onBoardAction match
        case moveAction: MoveAction =>
          val from = moveAction.from
          Validated.cond(isOccupied(board, from), board.piecesMap(from), UnoccupiedPosition)
            .andThen { piece =>
              Validated.cond(piece.owner == player, piece, NotOwnerOfPiece)
            }
        case dropAction: DropAction =>
          Validated.cond(isPlayerHandContains(board, player, dropAction.pieceType), getPieceByPieceType(dropAction.pieceType, player), NoPieceInHand)
            .andThen { piece =>
              Validated.cond(!isOccupied(board, dropAction.position), piece, OccupiedDrop)
            }.andThen { piece =>
              Validated.cond(piece.isInstanceOf[DroppablePiece], piece, InvalidDropPiece)
            }
    }

    def processAction(board: Board, player: Player, onBoardAction: OnBoardAction)(piece: Piece): (Piece, Validated[ActionValidationException, BoardStateTransition]) = {
      (piece, validateAndApplyAction(piece, board, onBoardAction))
    }

    def processGameEvent(inputTuple: (Piece, Validated[GameValidationException, BoardStateTransition])): Validated[GameValidationException, MoveResult] = {
      val (movingPiece, moveExecution) = inputTuple
      moveExecution.andThen { (board, stateTransitionList, algebraicNotation) =>
        val event = checkGameEvent(board, movingPiece.owner)

        val winnerCheckedBoard = board.copy(auxiliaryState = board.auxiliaryState.copy(gameWinner = event.winner))

        val newAlgebraicNotation = event.gameEvent match
          case Some(CHECK) | Some(CHECKMATE) => algebraicNotation + (if event.gameEvent.contains(CHECK) then " +" else " #")
          case Some(STALEMATE) | Some(IMPASSE) => algebraicNotation + " 1/2-1/2"
          case None => algebraicNotation
          case _ => algebraicNotation

        Valid((winnerCheckedBoard, stateTransitionList, algebraicNotation, event))
      }
    }

    def checkGameEvent(board: Board, player: Player): GameEventWinnerPair = {
      lazy val opponent = if (player == WHITE_PLAYER) BLACK_PLAYER else WHITE_PLAYER
      lazy val checked = isChecked(board, opponent)
      lazy val escape = hasEscape(board, opponent)
      lazy val stalemate = isStalemate(board, opponent)
      lazy val impasseWinner = determineImpasseWinner(board)

      (checked, escape, stalemate, impasseWinner) match {
        case (true, true, _, _) =>      GameEventWinnerPair(Some(CHECK),      None)
        case (true, false, _, _) =>     GameEventWinnerPair(Some(CHECKMATE),  Some(toWinner(player)))
        case (_, _, true, _) =>         GameEventWinnerPair(Some(STALEMATE),  Some(DRAW))
        case (_, _, _, Some(winner)) => GameEventWinnerPair(Some(IMPASSE),    Some(winner))
        case _ =>                       GameEventWinnerPair(None,             None)
      }
    }

    validateGameState(player)
      .andThen(_ => validateOnBoardAction(player, onBoardAction))
      .andThen(_ => validatePieceExistenceAndOwnership(player, onBoardAction))
      .andThen(processGameEvent compose processAction(board, player, onBoardAction))
  }

  def executeResignAction(board: Board, player: Player, resignAction: ResignAction): Validated[GameValidationException, MoveResult] = {
    val winner = toWinner(opponent(player))
    val gameEvent = GameEventWinnerPair(Some(RESIGNATION), Some(winner))
    val algebraicNotation = "1-0" // TODO: implement proper algebraic notation

    val newBoard = board.copy(
      auxiliaryState = board.auxiliaryState.copy(gameWinner = Some(winner))
    )
    Valid((newBoard, List.empty, algebraicNotation, gameEvent))
  }

  private def isOutOfTurn(currentPlayerTurn: Player, currentPlayer: Player): Boolean =
    currentPlayerTurn != currentPlayer

  private def isStalemate(board: Board, player: Player): Boolean = {
    !isChecked(board, player) && !(
      existsPlayerHands(board, player) { piece => piece.hasLegalDrop(board) } ||
      existsPlayerPieces(board, player) { (position, piece) =>piece.hasLegalMoves(board, position) }
    )
  }

  private def determineImpasseWinner(board: Board): Option[GameWinner] = {
    val whiteKingInZone = getKingPosition(board, WHITE_PLAYER).exists(_.y >= 7)
    val blackKingInZone = getKingPosition(board, BLACK_PLAYER).exists(_.y <= 3)

    def countScore(player: Player): Int = {
      val pieceScores = board.piecesMap.values.filter(_.owner == player).map(_.score).sum
      val handScores = board.hands.getOrElse(player, Multiset.empty).elements.map {
        case (pieceType, count) => getPieceByPieceType(pieceType, player).score * count
      }.sum
      pieceScores + handScores
    }


    if (whiteKingInZone && blackKingInZone) {
      if countScore(WHITE_PLAYER) > 30 then Some(WHITE_WINNER)
      else if countScore(BLACK_PLAYER) > 30 then Some(BLACK_WINNER)
      else Some(DRAW)
    }
    else None
  }

  def processAction(board: Board, actingPlayer: Player)(stateTransitionList: StateTransitionList): Board = {
    val updatedBoard = stateTransitionList.foldLeft(board) { (acc, transition) =>
      val (boardAction, position, player, pieceType) = (transition.boardAction, transition.position, transition.player, transition.piece)
      boardAction match {
        case BoardActionEnumerators.REMOVE =>
          acc.copy(piecesMap = acc.piecesMap - position)
        case BoardActionEnumerators.ADD =>
          val piece = PieceType.getPieceByPieceType(pieceType, player)
          acc.copy(piecesMap = acc.piecesMap + (position -> piece))
        case BoardActionEnumerators.HAND_ADD =>
          val updatedHand = acc.hands.getOrElse(player, Multiset.empty) + pieceType
          acc.copy(hands = acc.hands + (player -> updatedHand))
        case BoardActionEnumerators.HAND_REMOVE =>
          val updatedHand = acc.hands.getOrElse(player, Multiset.empty) - pieceType
          acc.copy(hands = acc.hands + (player -> updatedHand))
      }
    }
    updatedBoard.copy(currentPlayerTurn = opponent(actingPlayer))
  }

  def isChecked(board: Board, player: Player): Boolean = {
    val kingPosition = getKingPosition(board, player)
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
          currentPlayerTurn = opponent(player)
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
            currentPlayerTurn = opponent(player)
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
  
  def convertToBoardConfiguration(board: Board): BoardConfiguration = {
    val boardConfiguration = board.piecesMap.map {
      case (position, piece) =>
        PositionPiecePair(position, piece.pieceType, piece.owner)
    }.toList

    val handPieceCounts = board.hands.flatMap {
      case (player, multiset) =>
        multiset.elements.map {
          case (pieceType, count) =>
            PieceHandCount(player, pieceType, count)
        }
    }.toList
    
    BoardConfiguration(boardConfiguration, handPieceCounts, board.currentPlayerTurn)
  }

  def convertToBoardConfigurationEvent(board: Board, whitePlayer: UserModel, blackPlayer: UserModel): BoardConfigurationEvent = {
    val boardConfiguration = convertToBoardConfiguration(board)
    val playerList = PlayerList(whitePlayer, blackPlayer)

    BoardConfigurationEvent(playerList, boardConfiguration.board, boardConfiguration.handPieceCounts, board.currentPlayerTurn)
  }
}

