package com.shogi8017.app.services

import cats.data.{EitherT, NonEmptyList, Validated}
import cats.effect.std.Queue
import cats.effect.{Concurrent, IO}
import cats.syntax.all.*
import com.shogi8017.app.exceptions.*
import com.shogi8017.app.models.enumerators.ActionType
import com.shogi8017.app.models.{BoardHistoryModel, GameModel, InvitationModel, UserModel}
import com.shogi8017.app.repository.*
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
    val repetition = 30.seconds
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

    val invitationEitherT: EitherT[IO, Throwable, (InvitationModel, Boolean)] = for {
      invitation <- getInvitation(gameCertificate)
      result <- if (isInvitationComplete(invitation))
        EitherT.rightT[IO, Throwable]((invitation, false))
      else
        processInvitation(invitation, requestingUserId, gameCertificate)
    } yield result

    invitationEitherT.value.flatMap {
      case Right((invitation, true)) =>
        (for {
          whiteUser <- getUser(invitation.whitePlayerId)
          blackUser <- getUser(invitation.blackPlayerId)
          payload = Board.convertToBoardConfigurationEvent(
            Board.defaultInitialPosition,
            whiteUser,
            blackUser
          )
        } yield payload).value.flatMap {
          case Right(payload) =>
            publishToTopic(invitation.whitePlayerId, clientRegistry, payload) *>
              publishToTopic(invitation.blackPlayerId, clientRegistry, payload)
          case Left(error) =>
            publishToTopic(requestingUserId, clientRegistry, InvalidGameActionEvent(error.toString))
        }
      case Right((_, false)) =>
        IO.unit
      case Left(error) =>
        publishToTopic(requestingUserId, clientRegistry, InvalidGameActionEvent(error.toString))
    }
  }

  private def onMakeMoveRequest(request: MakeMoveRequestContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    processOnBoardAction(request, clientRegistry, request.payload.move)
  }


  private def onMakeDropRequest(request: MakeDropRequestContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    processOnBoardAction(request, clientRegistry, request.payload.drop)
  }

  private def onResignRequest(request: ResignRequestContext, clientRegistry: GameActionAPIRegistry): IO[Unit] = {
    val gameCertificate = request.context.gameCertificate
    val requestingUserId = request.context.user.userId
    val resignAction = request.payload

    case class ResignResultWithContestantsId(whiteId: String, blackId: String)

    val resignResult: EitherT[IO, Throwable, ResignResultWithContestantsId] = for {
      game <- getGame(gameCertificate)
      player <- validatePlayer(game.whiteUserId, game.blackUserId, requestingUserId)
      executionHistories <- getExecutionHistories(game)
      newMoveNumber = executionHistories.lastOption.fold(1)(_.actionNumber + 1)
      _ <- createExecutionHistories(game.boardId, ExecutionAction(player, ResignAction()), newMoveNumber)
    } yield ResignResultWithContestantsId(game.whiteUserId, game.blackUserId)

    resignResult.value.flatMap {
      case Right(result) =>
        notifyContestants(List.empty, Some(RESIGNATION), result.whiteId, result.blackId, clientRegistry)
      case Left(error) =>
        publishToTopic(requestingUserId, clientRegistry, InvalidGameActionEvent(error.toString))
    }
  }

  private def processOnBoardAction(
   requestContext: GameActionRequestContext,
   clientRegistry: GameActionAPIRegistry,
   action: OnBoardAction,
  ): IO[Unit] = {
    val gameCertificate = requestContext.context.gameCertificate
    val requestingUser = requestContext.context.user
    val requestingUserId = requestingUser.userId

    case class ResultWrapper(result: MoveResult, whiteId: String, blackId: String)

    val processingPipeline = for {
      game <- getGame(gameCertificate)
      player <- validatePlayer(game.whiteUserId, game.blackUserId, requestingUserId)
      executionHistories <- getExecutionHistories(game)
      board <- validateAndSetUpBoard(executionHistories, game.boardId)

      actionResult <- EitherT.fromEither[IO](
        Board.executeOnBoardAction(board, player, action).toEither
      )

      newMoveNumber = executionHistories.lastOption.fold(1)(_.actionNumber + 1)
      _ <- createExecutionHistories(
        game.boardId,
        ExecutionAction(player, action),
        newMoveNumber,
      )
    } yield ResultWrapper(actionResult, game.whiteUserId, game.blackUserId)

    processingPipeline.value.flatMap {
      case Right(ResultWrapper(result, whiteId, blackId)) =>
        notifyContestants(result._2, result._4, whiteId, blackId, clientRegistry)
      case Left(error) =>
        publishToTopic(requestingUserId, clientRegistry, InvalidGameActionEvent(error.toString))
    }
  }

  private def notifyContestants(
   transitionList: StateTransitionList,
   gameEvent: Option[GameEvent],
   whiteId: String,
   blackId: String,
   clientRegistry: GameActionAPIRegistry
  ): IO[Unit] = {
    val event = ExecutionActionEvent(transitionList, gameEvent)
    publishToTopic(blackId, clientRegistry, event) *>
      publishToTopic(whiteId, clientRegistry, event)
  }

  private def getInvitation(gameCertificate: String): EitherT[IO, Throwable, InvitationModel] = {
    val k = invitationRepository.getInvitationByGameCertificate(
      GetInvitationByGameCertificatePayload(gameCertificate)
    )
    EitherT(invitationRepository.getInvitationByGameCertificate(
      GetInvitationByGameCertificatePayload(gameCertificate)
    )).subflatMap {
      _.toRight(InvitationNotFound)
    }
  }

  private def updateInvitation(invitationModel: InvitationModel): EitherT[IO, Throwable, Unit] = {
    EitherT(
      invitationRepository.updateInvitation(UpdateInvitationPayload(invitationModel))
    ).void
  }

  private def getGame(gameCertificate: String): EitherT[IO, Throwable, GameModel] =
    EitherT(
      gameRepository.getGame(GetGamePayload(gameCertificate)
    )).subflatMap {
      _.toRight(GameNotFound)
    }

  private def getUser(userId: String): EitherT[IO, Throwable, UserModel] =
    EitherT(
      userRepository.getUserById(GetUserPayload(userId))
    ).subflatMap {
      _.toRight(UserNotFound)
    }

  private def createGame(gameCertificate: String, whiteUserId: String, blackUserId: String): EitherT[IO, Throwable, GameModel] =
    EitherT(
      gameRepository.createGame(CreateGamePayload(gameCertificate, whiteUserId, blackUserId))
    )

  private def getExecutionHistories(game: GameModel): EitherT[IO, Throwable, List[BoardHistoryModel]] =
    EitherT(boardHistoryRepository.getBoardHistories(GetGameHistoriesPayload(game.boardId)))

  private def createExecutionHistories(boardId: String, executionAction: ExecutionAction, moveNumber: Int): EitherT[IO, Throwable, BoardHistoryModel] =
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

    EitherT(boardHistoryRepository.createBoardHistory(payload))
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

  private def validateAndSetUpBoard(executionHistories: List[BoardHistoryModel], boardId: String): EitherT[IO, AppExceptionNel, Board] = {
    EitherT.fromEither[IO](
      executionHistories                                    // List[BoardHistoryModel]
        .map(ExecutionAction.convertToExecuteAction)        // List[Validated[InvalidBoardHistory, ExecutionAction]]
        .traverse(_.toValidatedNel)                         // ValidatedNel[InvalidBoardHistory, List[ExecutionAction]]
        .andThen(Board.fromExecutionList(_).toValidatedNel) // ValidatedNel[AppException, Board]
        .leftMap(e => AppExceptionNel(e))                   // Validated[AppExceptionNel, Board]
        .toEither                                           // Either[AppExceptionNel, Board]
    )
  }
}
