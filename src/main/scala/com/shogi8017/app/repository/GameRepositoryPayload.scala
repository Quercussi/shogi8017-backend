package com.shogi8017.app.repository

case class CreateGamePayload(gameCertificate: String, whiteUserId: String, blackUserId: String)

case class GetGamePayload(gameCertificate: String)