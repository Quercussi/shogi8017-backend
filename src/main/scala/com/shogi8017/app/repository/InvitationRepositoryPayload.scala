package com.shogi8017.app.repository

import com.shogi8017.app.models.InvitationModel

case class CreateInvitationPayload(blackPlayerId: String, whitePlayerId: String)

case class UpdateInvitationPayload(invitationModel: InvitationModel)

case class GetInvitationByGameCertificatePayload(gameCertificate: String)