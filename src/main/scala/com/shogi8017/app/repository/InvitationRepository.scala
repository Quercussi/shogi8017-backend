package com.shogi8017.app.repository

import cats.data.EitherT
import cats.effect.IO
import cats.effect.std.UUIDGen
import com.shogi8017.app.models.InvitationModel
import doobie.*
import doobie.implicits.*

class InvitationRepository(trx: Transactor[IO]) {
  def getInvitationByGameCertificate(payload: GetInvitationByGameCertificatePayload): EitherT[IO, Throwable, Option[InvitationModel]] = {
    val gameCertificate = payload.gameCertificate
    val query = sql"SELECT * FROM invitations WHERE gameCertificate = $gameCertificate".query[InvitationModel]
    EitherT(query.option.transact(trx).attempt)
  }

  def createInvitation(payload: CreateInvitationPayload): EitherT[IO, Throwable, InvitationModel] = {
    for {
      invitationUuid <- EitherT.liftF(UUIDGen.randomUUID[IO])
      gameCertificateUuid <- EitherT.liftF(UUIDGen.randomUUID[IO])
      invitationId = invitationUuid.toString
      gameCertificate = gameCertificateUuid.toString
      _ <- EitherT {
        sql"""
          INSERT INTO invitations (invitationId, gameCertificate, whitePlayerId, blackPlayerId, hasWhiteAccepted, hasBlackAccepted)
          VALUES ($invitationId, $gameCertificate, ${payload.whitePlayerId}, ${payload.blackPlayerId}, FALSE, FALSE)
        """.update.run.transact(trx).attempt.map {
          case Right(_) => Right(())
          case Left(error) => Left(error)
        }
      }
    } yield InvitationModel(invitationId, gameCertificate, payload.whitePlayerId, payload.blackPlayerId, hasWhiteAccepted = false, hasBlackAccepted = false)
  }

  def updateInvitation(payload: UpdateInvitationPayload): EitherT[IO, Throwable, Unit] = {
    val invitation = payload.invitationModel
    val query = sql"""
      UPDATE invitations
      SET gameCertificate = ${invitation.gameCertificate},
          whitePlayerId = ${invitation.whitePlayerId},
          blackPlayerId = ${invitation.blackPlayerId},
          hasWhiteAccepted = ${invitation.hasWhiteAccepted},
          hasBlackAccepted = ${invitation.hasBlackAccepted}
      WHERE invitationId = ${invitation.invitationId}
    """.update.run

    EitherT(query.transact(trx).attempt.map {
      case Right(_) => Right(())
      case Left(err) => Left(err)
    })
  }
}

object InvitationRepository {
  def of(trx: Transactor[IO]): InvitationRepository = new InvitationRepository(trx)
}