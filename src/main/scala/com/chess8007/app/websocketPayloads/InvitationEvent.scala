package com.chess8007.app.websocketPayloads

import cats.effect.IO
import com.chess8007.app.models.UserModel
import fs2.concurrent.Topic
import io.circe.{Encoder, Json}
import io.circe.generic.semiauto.deriveEncoder

import scala.collection.concurrent.TrieMap

sealed trait InvitationEvent

case class InvitationNotificationEvent(gameId: String, invitingUser: UserModel) extends InvitationEvent

case class InvitationInitializingEvent(gameId: String) extends InvitationEvent

case class InvalidInvitationEvent(errorMessage: String) extends InvitationEvent

case object KeepAliveInvitationAPI extends InvitationEvent

//case object DisconnectInvitationAPIEvent extends InvitationEvent
//
//case object KeepAliveInvitationAPIEvent extends InvitationEvent

object InvitationEvent {

  implicit val userModelEncoder: Encoder[UserModel] = deriveEncoder

  implicit val invitationNotificationEventEncoder: Encoder[InvitationNotificationEvent] = deriveEncoder
  implicit val invitationResponseEventEncoder: Encoder[InvitationInitializingEvent] = deriveEncoder
  implicit val invalidInvitationEventEncoder: Encoder[InvalidInvitationEvent] = deriveEncoder

  implicit val invitationEventEncoder: Encoder[InvitationEvent] = Encoder.instance {
    case notification: InvitationNotificationEvent =>
      Json.obj(
        "type" -> Json.fromString("InvitationNotification"),
        "event" -> invitationNotificationEventEncoder(notification)
      )
    case response: InvitationInitializingEvent =>
      Json.obj(
        "type" -> Json.fromString("InvitationResponse"),
        "event" -> invitationResponseEventEncoder(response)
      )
    case error: InvalidInvitationEvent => {
      Json.obj(
        "type" -> Json.fromString("InvalidInvitation"),
        "event" -> Json.obj(
          "errorMessage" -> invalidInvitationEventEncoder(error)
        )
      )
    }
  }
}

type InvitationAPIRegistry =  TrieMap[String, Topic[IO, InvitationEvent]]