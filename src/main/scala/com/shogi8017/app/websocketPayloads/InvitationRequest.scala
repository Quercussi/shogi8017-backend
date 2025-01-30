package com.shogi8017.app.websocketPayloads

import cats.syntax.functor._
import io.circe.{Decoder, Json}
import io.circe.generic.semiauto.deriveDecoder

/**
 * A trait that represents the body of all WebSocket requests via the `/ws/invite` API.
 * All WebSocket requests that are part of the `/ws/invite` API must extend this trait.
 */
sealed trait InvitationRequest extends WebSocketRequestBody

/**
 * A case class representing a regular invitation request with a user ID.
 * This is used when a user is invited to a game.
 *
 * @param userId The ID of the user being invited.
 */
case class RegularInvitationBody(userId: String) extends InvitationRequest

case class InvalidInvitationBody(errorMessage: String) extends InvitationRequest

case object DisconnectInvitationAPI extends InvitationRequest

object InvitationRequest {

  /**
   * A decoder for `InvitationRequest` that dispatches to the specific decoders based on the action field.
   */
  implicit val invitationRequestDecoder: Decoder[InvitationRequest] = Decoder.instance { cursor =>
    WebSocketRequestBody.dispatchDecoder[InvitationRequest](
      cursor,
      {
        case "invite" =>  deriveDecoder[RegularInvitationBody].widen
      }
    )
  }
  
  def decoder: Decoder[InvitationRequest] = invitationRequestDecoder
}