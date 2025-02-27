package com.shogi8017.app.services

import cats.effect.std.Queue
import cats.effect.{Concurrent, IO}
import com.shogi8017.app.repository.{CreateInvitationPayload, InvitationRepository}
import com.shogi8017.app.websocketPayloads.*
import fs2.Stream
import fs2.concurrent.Topic

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.*

class InvitationService(invitationRepository: InvitationRepository) {
  def initiateProcessingStream(queue: Queue[IO, InvitationRequestContext], clientRegistry: InvitationAPIRegistry): Stream[IO, Unit] = {
    Stream
      .repeatEval(queue.take)
      .evalMap {
        case WebSocketBodyContext(context, payload: RegularInvitationBody) =>
          onRegularInvitationBody(WebSocketBodyContext(context, payload), clientRegistry)

        case WebSocketBodyContext(context, payload: DisconnectInvitationAPI) =>
          onDisconnectInvitationAPI(WebSocketBodyContext(context, payload), clientRegistry)

        case WebSocketBodyContext(context, payload: InvalidInvitationBody) =>
          onInvalidInvitationBody(WebSocketBodyContext(context, payload), clientRegistry)

        case _ => Concurrent[IO].unit
      }
      .concurrently(pingingStream(clientRegistry))
      .handleErrorWith { error =>
        Stream.eval(IO(println(s"Error processing message: $error"))).drain
      }
  }

  private def pingingStream(clientRegistry: TrieMap[String, Topic[IO, InvitationEvent]]) = {
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

  private def onRegularInvitationBody(request: RegularInvitationBodyContext, clientRegistry: InvitationAPIRegistry): IO[Unit] = {
    val (inviter, inviteeId) = (request.context, request.payload.userId)
    val invitationModelEitherT = invitationRepository.createInvitation(CreateInvitationPayload(inviter.userId, inviteeId))
    invitationModelEitherT.value.flatMap {
      case Right(invitationModel) =>
        publishToTopic(request.context.userId, clientRegistry, InvitationInitializingEvent(invitationModel.gameCertificate)) *>
        publishToTopic(inviteeId, clientRegistry, InvitationNotificationEvent(invitationModel.gameCertificate, inviter))
      case Left(e) => 
        publishToTopic(request.context.userId, clientRegistry, InvalidInvitationEvent(e.toString))
    }
  }

  private def onDisconnectInvitationAPI(request: DisconnectInvitationAPIContext, clientRegistry: InvitationAPIRegistry): IO[Unit] = {
    clientRegistry.remove(request.context.userId)
    Concurrent[IO].unit
  }

  private def onInvalidInvitationBody(request: InvalidInvitationBodyContext, clientRegistry: InvitationAPIRegistry): IO[Unit] = {
    val (inviterId, errorMessage) = (request.context.userId, request.payload.errorMessage)
    publishToTopic(inviterId, clientRegistry, InvalidInvitationEvent(s"Invalid invitation body: $errorMessage"))
  }
}

object InvitationService {
  def of(invitationRepository: InvitationRepository): InvitationService = {
    new InvitationService(invitationRepository)
  }
}
