package com.shogi8017.app.services

import cats.data.{EitherT, NonEmptyList, Validated}
import cats.effect.std.Queue
import cats.effect.{Concurrent, IO}
import cats.syntax.all.*
import com.shogi8017.app.exceptions.*
import com.shogi8017.app.models.enumerators.{ActionType, GameState, GameWinner}
import com.shogi8017.app.models.{BoardHistoryModel, GameModel, InvitationModel, UserModel}
import com.shogi8017.app.repository.*
import com.shogi8017.app.routes.MoveResultReduced
import com.shogi8017.app.services.GameActionService.{validateAndSetUpBoard, validatePlayer}
import com.shogi8017.app.services.logics.GameEvent.RESIGNATION
import com.shogi8017.app.services.logics.Player.*
import com.shogi8017.app.services.logics.actions.*
import com.shogi8017.app.services.logics.*
import com.shogi8017.app.websocketPayloads.*
import fs2.Stream
import fs2.concurrent.Topic

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.*

class GameActionService(gameRepository: GameRepository, invitationRepository: InvitationRepository, boardHistoryRepository: BoardHistoryRepository, userRepository: UserRepository) {
  def initiateProcessingStream(queue: Queue[IO, GameActionRequestContext], clientRegistry: GameActionAPIRegistry): Stream[IO, Unit] = {
    Stream
      .repeatEval(queue.take)
      .evalMap {
        case WebSocketBodyContext(context, payload: MakeMoveRequest) =>
          onMakeMoveRequest(WebSocketBodyContext(context, payload), clientRegistry)

        case WebSocketBodyContext(context, payload: MakeDropRequest) =>
          onMakeDropRequest(WebSocketBodyContext(context, payload), clientRegistry)

        case WebSocketBodyContext(context, payload: ResignRequest) =>
          onResignRequest(WebSocketBodyContext(context, payload), clientRegistry)

        case WebSocketBodyContext(context, payload: ConnectGameActionAPI) =>
          onConnectGameActionAPI(WebSocketBodyContext(context, payload), clientRegistry)

        case WebSocketBodyContext(context, payload: DisconnectGameActionAPI) =>
          onDisconnectGameActionAPI(WebSocketBodyContext(context, payload), clientRegistry)

        case WebSocketBodyContext(context, payload: InvalidGameActionBody) =>
          onInvalidGameActionBody(WebSocketBodyContext(context, payload), clientRegistry)

        case _ => Concurrent[IO].unit
      }
      .concurrently(pingingStream(clientRegistry))
      .handleErrorWith { error =>
        Stream.eval(IO(println(s"Error processing message: $error"))).drain
      }
  }

  private def pingingStream(clientRegistry: TrieMap[String, Topic[IO, GameActionEvent]]) = {
    val repetition = 10.seconds
    Stream
      .awakeEvery[IO](repetition)
      .evalMap { _ =>
        val topics = clientRegistry.values.toList
        val totalTopics = topics.size

        if (totalTopics > 0) {
          val delayBetweenMessages = repetition / totalTopics

          Stream
            .emits(topics)
            .zipWithIndex
            .evalMap { case (topic, index) =>
              IO.sleep(delayBetweenMessages * (index + 1)) >> topic.publish1(KeepAliveGameActionAPI)
            }
            .compile
            .drain
        } else {
          IO.unit
        }
      }
  }

  private def publishToTopic(userId: String, clientRegistry: GameActionAPIRegistry, event: GameActionEvent): IO[Unit] = {
    clientRegistry.get(userId) match {
      case Some(topic) => topic.publish1(event).void
      case None => Concurrent[IO].unit
    }
  }

  private def onDisconnectGameActionAPI(request: DisconnectGameActionAPIContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    clientRegistry.remove(request.context.user.userId)
    Concurrent[IO].unit
  }

  private def onInvalidGameActionBody(request: InvalidGameActionBodyContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    publishToTopic(request.context.user.userId, clientRegistry, InvalidGameActionEvent(s"Invalid gameAction body: ${request.payload.errorMessage}"))
  }

  private def onConnectGameActionAPI(request: ConnectGameActionAPIContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    val gameCertificate = request.context.gameCertificate
    val requestingUser = request.context.user
    val requestingUserId = requestingUser.userId


    def isInvitationComplete(invitation: InvitationModel): Boolean =
      invitation.hasBlackAccepted && invitation.hasWhiteAccepted

    def updateInvitationStatus(invitation: InvitationModel, requestingUserId: String): Either[Throwable, InvitationModel] =
      if (invitation.blackPlayerId == requestingUserId)
        Right(invitation.copy(hasBlackAccepted = true))
      else if (invitation.whitePlayerId == requestingUserId)
        Right(invitation.copy(hasWhiteAccepted = true))
      else
        Left(UserNotInvited)

    def createGameIfReady(invitation: InvitationModel, gameCertificate: String): EitherT[IO, Throwable, Boolean] =
      if (isInvitationComplete(invitation))
        createGame(gameCertificate, invitation.whitePlayerId, invitation.blackPlayerId).map(_ => true)
      else
        EitherT.pure[IO, Throwable](false)

    def processInvitation(invitation: InvitationModel, requestingUserId: String, gameCertificate: String): EitherT[IO, Throwable, (InvitationModel, Boolean)] =
      for {
        updatedInvitation <- EitherT.fromEither[IO](updateInvitationStatus(invitation, requestingUserId))
        _ <- updateInvitation(updatedInvitation)
        hasGameCreated <- createGameIfReady(updatedInvitation, gameCertificate)
      } yield (updatedInvitation, hasGameCreated)

    def notifyPlayersBoardConfiguration(invitation: InvitationModel, recipients: List[String]): IO[Unit] = {
      val currentBoard = for {
        whiteUser <- getUser(invitation.whitePlayerId)
        blackUser <- getUser(invitation.blackPlayerId)
        game <- getGameEither(invitation.gameCertificate)
        executionHistories <- getExecutionHistories(game)
        board <- EitherT.fromEither[IO](validateAndSetUpBoard(executionHistories, game.boardId))
        payload <- EitherT.liftF(IO(Board.convertToBoardConfigurationEvent(
          board,
          whiteUser,
          blackUser
        )))
      } yield payload

      currentBoard.value.flatMap {
        case Right(payload) =>
          recipients.traverse_ { userId =>
            publishToTopic(userId, clientRegistry, payload)
          }
        case Left(error) =>
          publishToTopic(requestingUserId, clientRegistry, InvalidGameActionEvent(error.toString))
      }
    }

    val invitationEitherT: EitherT[IO, Throwable, (InvitationModel, Boolean)] = for {
      invitation <- getInvitation(gameCertificate)
      result <- if (isInvitationComplete(invitation))
                  EitherT.rightT[IO, Throwable]((invitation, false))
                else
                  processInvitation(invitation, requestingUserId, gameCertificate)
    } yield result

    invitationEitherT.value.flatMap {
      case Right((invitation, true)) =>
        notifyPlayersBoardConfiguration(invitation, List(invitation.whitePlayerId, invitation.blackPlayerId))
      case Right((invitation, false)) =>
        notifyPlayersBoardConfiguration(invitation, List(requestingUserId))
      case Left(error) =>
        publishToTopic(requestingUserId, clientRegistry, InvalidGameActionEvent(error.toString))
    }
  }

  private def onMakeMoveRequest(request: MakeMoveRequestContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    onBoardExecutionAction(request, clientRegistry, request.payload.move)
  }

  private def onMakeDropRequest(request: MakeDropRequestContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    onBoardExecutionAction(request, clientRegistry, request.payload.drop)
  }

  private def onResignRequest(request: ResignRequestContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    val requestingUserId = request.context.user.userId

    processResignation(request.context.gameCertificate, requestingUserId)
      .value
      .flatMap {
        case Right(result) =>
          val gameEventWinnerPair = GameEventWinnerPair(Some(RESIGNATION), Some(result.winner))
          notifyContestants(List.empty, gameEventWinnerPair, result.whiteId, result.blackId, clientRegistry)
        case Left(error) =>
          publishToTopic(requestingUserId, clientRegistry, InvalidGameActionEvent(error.toString))
      }
  }

  private case class ResignResult(whiteId: String, blackId: String, resigningPlayer: Player, winner: GameWinner)

  private def processResignation(gameCertificate: String, userId: String): EitherT[IO, Throwable, ResignResult] =
    for {
      game <- getGameEither(gameCertificate)
      resigningPlayer <- validatePlayer(game.whiteUserId, game.blackUserId, userId)
      _ <- recordResignation(game.boardId, resigningPlayer)
      winner = toWinner(opponent(resigningPlayer))
      _ <- finalizeGame(game, winner)
    } yield ResignResult(game.whiteUserId, game.blackUserId, resigningPlayer, winner)

  private def recordResignation(boardId: String, player: Player): EitherT[IO, Throwable, BoardHistoryModel] =
    for {
      histories <- boardHistoryRepository.getBoardHistories(GetBoardHistoriesPayload(boardId))
      moveNumber = histories.lastOption.fold(1)(_.actionNumber + 1)
      result <- createExecutionHistories(boardId, ExecutionAction(player, ResignAction()), moveNumber)
    } yield result

  private def finalizeGame(game: GameModel, winner: GameWinner): EitherT[IO, Throwable, GameModel] = {
    val updatedGame = game.copy(gameState = GameState.FINISHED, winner = Some(winner))
    gameRepository.updateGame(updatedGame)
  }

  private def onBoardExecutionAction(requestContext: GameActionRequestContext, clientRegistry: GameActionAPIRegistry, action: OnBoardAction): IO[Unit] = {
    val requestingUserId = requestContext.context.user.userId

    processOnBoardAction(requestContext.context.gameCertificate, requestingUserId, action)
      .value
      .flatMap {
        case Right(result) =>
          notifyContestants(result.transitions, result.gameEvent, result.whiteId, result.blackId, clientRegistry)
        case Left(error) =>
          publishToTopic(requestingUserId, clientRegistry, InvalidGameActionEvent(error.toString))
      }
  }

  private case class BoardActionResult(transitions: StateTransitionList, gameEvent: GameEventWinnerPair, whiteId: String, blackId: String)
  
  private def processOnBoardAction(gameCertificate: String, userId: String, action: OnBoardAction): EitherT[IO, Throwable, BoardActionResult] =
    for {
      game <- getGameEither(gameCertificate)
      player <- validatePlayer(game.whiteUserId, game.blackUserId, userId)
      executionHistories <- getExecutionHistories(game)
      board <- EitherT.fromEither[IO](validateAndSetUpBoard(executionHistories, game.boardId))

      actionResult <- EitherT.fromEither[IO](executeAction(board, player, action))

      _ <- updateGameHistory(game, player, action, actionResult)
      _ <- handleGameEndingConditions(game, actionResult._4)
    } yield BoardActionResult(transitions = actionResult._2, gameEvent = actionResult._4, whiteId = game.whiteUserId, blackId = game.blackUserId)

  private def executeAction(board: Board, player: Player, action: OnBoardAction): Either[Throwable, MoveResult] =
    Board.executeOnBoardAction(board, player, action).toEither

  private def updateGameHistory(game: GameModel, player: Player, action: OnBoardAction, actionResult: MoveResult): EitherT[IO, Throwable, BoardHistoryModel] =
    for {
      executionHistories <- getExecutionHistories(game)
      newMoveNumber = executionHistories.lastOption.fold(1)(_.actionNumber + 1)
      historyEntry <- createExecutionHistories(
        game.boardId,
        ExecutionAction(player, action),
        newMoveNumber
      )
    } yield historyEntry
  
  private def handleGameEndingConditions(game: GameModel, gameEventWinnerPair: GameEventWinnerPair): EitherT[IO, Throwable, Unit] = {
    val gameEndingEvents = Set(GameEvent.STALEMATE, GameEvent.IMPASSE, GameEvent.CHECKMATE, GameEvent.RESIGNATION)

    gameEventWinnerPair.gameEvent
      .filter(gameEndingEvents.contains)
      .map { _ =>
        val updatedGame = game.copy(gameState = GameState.FINISHED, winner = gameEventWinnerPair.winner)
        gameRepository.updateGame(updatedGame).void
      }
      .getOrElse(EitherT.pure[IO, Throwable](()))
  }

  private def notifyContestants(
   transitionList: StateTransitionList,
   gameEvent: GameEventWinnerPair,
   whiteId: String,
   blackId: String,
   clientRegistry: GameActionAPIRegistry
  ): IO[Unit] = {
    val event = ExecutionActionEvent(transitionList, gameEvent)
    publishToTopic(blackId, clientRegistry, event) *>
      publishToTopic(whiteId, clientRegistry, event)
  }

  private def getInvitation(gameCertificate: String): EitherT[IO, Throwable, InvitationModel] = {
    invitationRepository.getInvitationByGameCertificate(
      GetInvitationByGameCertificatePayload(gameCertificate)
    ).subflatMap(_.toRight(InvitationNotFound))
  }

  private def updateInvitation(invitationModel: InvitationModel): EitherT[IO, Throwable, Unit] = {
    invitationRepository.updateInvitation(UpdateInvitationPayload(invitationModel))
  }

  private def getGameEither(gameCertificate: String): EitherT[IO, Throwable, GameModel] =
    gameRepository.getGame(GetGamePayload(gameCertificate))
      .subflatMap(_.toRight(GameNotFound))

  private def getUser(userId: String): EitherT[IO, Throwable, UserModel] =
    userRepository.getUserById(GetUserByIdPayloadRepo(userId))
      .subflatMap(_.toRight(UserNotFound))

  private def createGame(gameCertificate: String, whiteUserId: String, blackUserId: String): EitherT[IO, Throwable, GameModel] =
    gameRepository.createGame(CreateGamePayload(gameCertificate, whiteUserId, blackUserId))

  private def getExecutionHistories(game: GameModel): EitherT[IO, Throwable, List[BoardHistoryModel]] =
    boardHistoryRepository.getBoardHistories(GetBoardHistoriesPayload(game.boardId))

  private def createExecutionHistories(boardId: String, executionAction: ExecutionAction, moveNumber: Int): EitherT[IO, Throwable, BoardHistoryModel] = {
    val payload = executionAction.playerAction match {
      case MoveAction(from, to, toPromote) =>
        CreateBoardHistoryPayload(
          boardId,
          moveNumber,
          ActionType.MOVE,
          Some(from.x),
          Some(from.y),
          Some(to.x),
          Some(to.y),
          None,
          toPromote,
          executionAction.player
        )

      case DropAction(position, pieceType) =>
        CreateBoardHistoryPayload(
          boardId,
          moveNumber,
          ActionType.DROP,
          None,
          None,
          Some(position.x),
          Some(position.y),
          Some(pieceType),
          false,
          executionAction.player
        )

      case ResignAction() =>
        CreateBoardHistoryPayload(
          boardId,
          moveNumber,
          ActionType.RESIGN,
          None,
          None,
          None,
          None,
          None,
          false,
          executionAction.player
        )
    }

    boardHistoryRepository.createBoardHistory(payload)
  }
}

object GameActionService {
  def of(gameRepository: GameRepository, invitationRepository: InvitationRepository, boardHistoryRepository: BoardHistoryRepository, userRepository: UserRepository): GameActionService = {
    new GameActionService(gameRepository, invitationRepository, boardHistoryRepository, userRepository)
  }

  private def validatePlayer(whiteId: String, blackId: String, requestingUserId: String): EitherT[IO, AppExceptionNel, Player] =
    EitherT.cond[IO](
      requestingUserId == whiteId || requestingUserId == blackId,
      if (requestingUserId == whiteId) WHITE_PLAYER else BLACK_PLAYER,
      AppExceptionNel.singleton(UserNotInGame)
    )

  private def validateAndSetUpBoard(executionHistories: List[BoardHistoryModel], boardId: String): Either[AppExceptionNel, Board] = {
    convertBoardHistoryToExecutionActionList(executionHistories).flatMap { actions =>
      Board.fromExecutionList(actions)
        .toValidatedNel
        .leftMap(e => AppExceptionNel(e))
        .toEither
    }
  }
  
  def convertBoardHistoryToExecutionActionList(executionHistories: List[BoardHistoryModel]): Either[AppExceptionNel, List[ExecutionAction]] = {
    executionHistories                                    
      .map(ExecutionAction.convertToExecuteAction)        
      .traverse(_.toValidatedNel)
      .leftMap(e => AppExceptionNel(e))
      .toEither
  }

  def convertExecutionActionsToMoveResults(actions: List[ExecutionAction]): Either[GameValidationException, List[MoveResultReduced]] = {
    type EitherGVE[A] = Either[GameValidationException, A]
    
    actions.foldLeft[Either[GameValidationException, (Board, List[MoveResultReduced])]](Right((Board.defaultInitialPosition, List.empty))) {
      case (acc, action) =>
        acc.flatMap { case (currentBoard, results) =>
          Board.executionAction(currentBoard, action.player, action.playerAction).toEither.map { moveResult =>
            val (newBoard, stateTransitions, algebraicNotation, gameEvent) = moveResult
            val reducedResult = MoveResultReduced(action.player, stateTransitions, algebraicNotation, gameEvent)
            (newBoard, results :+ reducedResult)
          }
        }
    }.map(_._2)
  }
}