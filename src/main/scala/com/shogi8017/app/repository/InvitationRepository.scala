package com.shogi8017.app.repository

import cats.effect.IO
import cats.effect.std.UUIDGen
import com.shogi8017.app.models.InvitationModel
import doobie.*
import doobie.implicits.*

class InvitationRepository(trx: Transactor[IO]) {
  def getInvitationByGameCertificate(payload: GetInvitationByGameCertificatePayload): IO[Either[Throwable, Option[InvitationModel]]] = {
    val gameCertificate = payload.gameCertificate
    val query: Query0[InvitationModel] = sql"SELECT * FROM invitations u WHERE u.gameCertificate = $gameCertificate".query[InvitationModel]
    query.option.transact(trx).attempt
  }

  def createInvitation(payload: CreateInvitationPayload): IO[Either[Throwable, InvitationModel]] = {
    for {
      invitationUuid <- UUIDGen.randomUUID[IO]
      gameCertificateUuid <- UUIDGen.randomUUID[IO]
      invitationId = invitationUuid.toString
      gameCertificate = gameCertificateUuid.toString
      result <-
        sql"""
        INSERT INTO invitations (invitationId, gameCertificate, whitePlayerId, blackPlayerId, hasWhiteAccepted, hasBlackAccepted)
        VALUES ($invitationId, $gameCertificate, ${payload.whitePlayerId}, ${payload.blackPlayerId}, FALSE, FALSE)
      """.update.run.transact(trx).attempt
    } yield result match {
      case Right(_) => Right(InvitationModel(invitationId, gameCertificate, payload.whitePlayerId, payload.blackPlayerId, false, false))
      case Left(error) => Left(error)
    }
  }

  def updateInvitation(payload: UpdateInvitationPayload): IO[Either[Throwable, Unit]] = {
    val invitation = payload.invitationModel

    val query: Update0 =
      sql"""
      UPDATE invitations
      SET gameCertificate = ${invitation.gameCertificate},
          whitePlayerId = ${invitation.whitePlayerId},
          blackPlayerId = ${invitation.blackPlayerId},
          hasWhiteAccepted = ${invitation.hasWhiteAccepted},
          hasBlackAccepted = ${invitation.hasBlackAccepted}
      WHERE invitationId = ${invitation.invitationId}
    """.update

    query.run.transact(trx).attempt.map {
      case Right(_) => Right(())
      case Left(err) => Left(err)
    }
  }
}

object InvitationRepository {
  def of(trx: Transactor[IO]): InvitationRepository = new InvitationRepository(trx)
}