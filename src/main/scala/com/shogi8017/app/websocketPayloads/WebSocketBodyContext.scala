package com.shogi8017.app.websocketPayloads

import com.shogi8017.app.models.UserModel
import com.shogi8017.app.routes.GameUserPair

case class WebSocketBodyContext[T, +U](context: T, payload: U)

type InvitationRequestContext = WebSocketBodyContext[UserModel, InvitationRequest]
type RegularInvitationBodyContext = WebSocketBodyContext[UserModel, RegularInvitationBody]
type InvalidInvitationBodyContext = WebSocketBodyContext[UserModel, InvalidInvitationBody]
type DisconnectInvitationAPIContext = WebSocketBodyContext[UserModel, DisconnectInvitationAPI]

type GameActionRequestContext = WebSocketBodyContext[GameUserPair, GameActionRequest]
type MakeMoveRequestContext = WebSocketBodyContext[GameUserPair, MakeMoveRequest]
type MakeDropRequestContext = WebSocketBodyContext[GameUserPair, MakeDropRequest]
type ResignRequestContext = WebSocketBodyContext[GameUserPair, ResignRequest]
type ConnectGameActionAPIContext = WebSocketBodyContext[GameUserPair, ConnectGameActionAPI]
type DisconnectGameActionAPIContext = WebSocketBodyContext[GameUserPair, DisconnectGameActionAPI]
type InvalidGameActionBodyContext = WebSocketBodyContext[GameUserPair, InvalidGameActionBody]