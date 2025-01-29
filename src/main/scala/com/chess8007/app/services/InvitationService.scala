package com.chess8007.app.services

import cats.effect.std.Queue
import cats.effect.{Concurrent, IO}
import com.chess8007.app.repository.{CreateGamePayload, GameRepository}
import com.chess8007.app.websocketPayloads.*
import fs2.Stream
import fs2.concurrent.Topic

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.*

class InvitationService(gameRepository: GameRepository) {
  def initiateProcessingStream(queue: Queue[IO, InvitationRequestContext], clientRegistry: InvitationAPIRegistry): Stream[IO, Unit] = {
    Stream
      .repeatEval(queue.take)
      .evalMap { context =>
        context.payload match {
          case RegularInvitationBody(userId) => onRegularInvitationBody(context, clientRegistry, userId)
          case DisconnectInvitationAPI => onDisconnectInvitationAPI(context, clientRegistry)
          case InvalidInvitationBody(errorMessage) => onInvalidInvitationBody(context, clientRegistry, errorMessage)
          case _ => Concurrent[IO].unit
        }
      }
      .concurrently(pingingStream(clientRegistry))
      .handleErrorWith { error =>
        Stream.eval(IO(println(s"Error processing message: $error"))).drain
      }
  }

  private def pingingStream(clientRegistry: TrieMap[String, Topic[IO, InvitationEvent]]) = {
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
              IO.sleep(delayBetweenMessages * (index + 1)) >> topic.publish1(KeepAliveInvitationAPI)
            }
            .compile
            .drain
        } else {
          IO.unit
        }
      }
  }

  private def publishToTopic(userId: String, clientRegistry: InvitationAPIRegistry, event: InvitationEvent): IO[Unit] = {
    clientRegistry.get(userId) match {
      case Some(topic) => topic.publish1(event).void
      case None => Concurrent[IO].unit
    }
  }

  private def onRegularInvitationBody(context: InvitationRequestContext, clientRegistry: InvitationAPIRegistry, inviteeId: String): IO[Unit] = {
    val requestingUser= context.requestingUser
    val gameModelEither = gameRepository.createGame(CreateGamePayload(requestingUser.userId, inviteeId))
    gameModelEither.flatMap {
      case Right(gameModel) =>
        publishToTopic(context.requestingUser.userId, clientRegistry, InvitationInitializingEvent(gameModel.gameId)) *>
        publishToTopic(inviteeId, clientRegistry, InvitationNotificationEvent(gameModel.gameId, requestingUser))
      case Left(e) => 
        publishToTopic(context.requestingUser.userId, clientRegistry, InvalidInvitationEvent(e.toString))
    }
  }

  private def onDisconnectInvitationAPI(context: InvitationRequestContext, clientRegistry: InvitationAPIRegistry): IO[Unit] = {
    clientRegistry.remove(context.requestingUser.userId)
    Concurrent[IO].unit
  }

  private def onInvalidInvitationBody(context: InvitationRequestContext, clientRegistry: InvitationAPIRegistry, errorMessage: String): IO[Unit] = {
    publishToTopic(context.requestingUser.userId, clientRegistry, InvalidInvitationEvent(s"Invalid invitation body: $errorMessage"))
  }
}

object InvitationService {
  def of(gameRepository: GameRepository): InvitationService = {
    new InvitationService(gameRepository)
  }
}
