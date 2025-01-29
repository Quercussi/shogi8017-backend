package com.chess8007.app.websocketPayloads

import com.chess8007.app.models.UserModel

case class WebSocketBodyContext[T](requestingUser: UserModel, payload: T)

type InvitationRequestContext = WebSocketBodyContext[InvitationRequest]
type RegularInvitationBodyContext = WebSocketBodyContext[RegularInvitationBody]

type GameEventContext = WebSocketBodyContext[GameEventRequest]
type MakeMoveRequestContext = WebSocketBodyContext[MakeMoveRequest]
type ResignRequestContext = WebSocketBodyContext[ResignRequest]
type DrawRequestContext = WebSocketBodyContext[DrawRequest]