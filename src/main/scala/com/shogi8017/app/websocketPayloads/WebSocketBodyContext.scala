package com.shogi8017.app.websocketPayloads

import com.shogi8017.app.models.UserModel

case class WebSocketBodyContext[T](requestingUser: UserModel, payload: T)

type InvitationRequestContext = WebSocketBodyContext[InvitationRequest]
type RegularInvitationBodyContext = WebSocketBodyContext[RegularInvitationBody]

type GameEventContext = WebSocketBodyContext[GameEventRequest]
type MakeMoveRequestContext = WebSocketBodyContext[MakeMoveRequest]
type ResignRequestContext = WebSocketBodyContext[ResignRequest]
type DrawRequestContext = WebSocketBodyContext[DrawRequest]