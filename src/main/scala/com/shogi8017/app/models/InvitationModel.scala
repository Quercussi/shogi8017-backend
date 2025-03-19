
package com.shogi8017.app.models

case class InvitationModel(
  invitationId: String,
  gameCertificate: String,
  whitePlayerId: String,
  blackPlayerId: String,
  hasWhiteAccepted: Boolean,
  hasBlackAccepted: Boolean,
)