package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.Player
import doobie.util.{Get, Put}
import io.circe.{Decoder, Encoder}

sealed trait PieceType

enum PromotablePieceType extends PieceType:
  case ROOK, BISHOP, LANCE, KNIGHT, SILVER, PAWN

enum PromotedPieceType extends PieceType:
  case P_ROOK, P_BISHOP, P_LANCE, P_KNIGHT, P_SILVER, P_PAWN

enum UnPromotablePieceType extends PieceType:
  case KING, GOLD
  
sealed trait DroppablePieceType extends PieceType

object PieceType {
  def getPieceByPieceType(piece: PieceType, player: Player): Piece = {
    piece match {
      case PromotablePieceType.ROOK => Rook(player)
      case PromotablePieceType.BISHOP => Bishop(player)
      case PromotablePieceType.LANCE => Lance(player)
      case PromotablePieceType.KNIGHT => Knight(player)
      case PromotablePieceType.SILVER => Silver(player)
      case PromotablePieceType.PAWN => Pawn(player)
      case PromotedPieceType.P_ROOK => PromotedRook(player)
      case PromotedPieceType.P_BISHOP => PromotedBishop(player)
      case PromotedPieceType.P_LANCE => PromotedLance(player)
      case PromotedPieceType.P_KNIGHT => PromotedKnight(player)
      case PromotedPieceType.P_SILVER => PromotedSilver(player)
      case PromotedPieceType.P_PAWN => PromotedPawn(player)
      case UnPromotablePieceType.KING => King(player)
      case UnPromotablePieceType.GOLD => Gold(player)
    }
  }
  
  implicit val pieceTypeDecoder: Decoder[PieceType] = Decoder[String].emap {
    case "ROOK" => Right(PromotablePieceType.ROOK)
    case "BISHOP" => Right(PromotablePieceType.BISHOP)
    case "LANCE" => Right(PromotablePieceType.LANCE)
    case "KNIGHT" => Right(PromotablePieceType.KNIGHT)
    case "SILVER" => Right(PromotablePieceType.SILVER)
    case "PAWN" => Right(PromotablePieceType.PAWN)
    case "P_ROOK" => Right(PromotedPieceType.P_ROOK)
    case "P_BISHOP" => Right(PromotedPieceType.P_BISHOP)
    case "P_LANCE" => Right(PromotedPieceType.P_LANCE)
    case "P_KNIGHT" => Right(PromotedPieceType.P_KNIGHT)
    case "P_SILVER" => Right(PromotedPieceType.P_SILVER)
    case "P_PAWN" => Right(PromotedPieceType.P_PAWN)
    case "KING" => Right(UnPromotablePieceType.KING)
    case "GOLD" => Right(UnPromotablePieceType.GOLD)
    case other => Left(s"Unknown PieceType: $other")
  }

  implicit val pieceTypeGet: Get[PieceType] = Get[String].temap {
    case "ROOK" => Right(PromotablePieceType.ROOK)
    case "BISHOP" => Right(PromotablePieceType.BISHOP)
    case "LANCE" => Right(PromotablePieceType.LANCE)
    case "KNIGHT" => Right(PromotablePieceType.KNIGHT)
    case "SILVER" => Right(PromotablePieceType.SILVER)
    case "PAWN" => Right(PromotablePieceType.PAWN)
    case "P_ROOK" => Right(PromotedPieceType.P_ROOK)
    case "P_BISHOP" => Right(PromotedPieceType.P_BISHOP)
    case "P_LANCE" => Right(PromotedPieceType.P_LANCE)
    case "P_KNIGHT" => Right(PromotedPieceType.P_KNIGHT)
    case "P_SILVER" => Right(PromotedPieceType.P_SILVER)
    case "P_PAWN" => Right(PromotedPieceType.P_PAWN)
    case "KING" => Right(UnPromotablePieceType.KING)
    case "GOLD" => Right(UnPromotablePieceType.GOLD)
    case other => Left(s"Invalid PieceType: $other")
  }

  implicit val pieceTypePut: Put[PieceType] = Put[String].contramap(_.toString)

  implicit val pieceTypeEncoder: Encoder[PieceType] = Encoder[String].contramap {
    case piece: PromotablePieceType => piece.toString
    case piece: PromotedPieceType => piece.toString
    case piece: UnPromotablePieceType => piece.toString
  }
}

object PromotablePieceType {
  def promote(piece: PromotablePieceType): PromotedPieceType = {
    piece match {
      case PromotablePieceType.ROOK => PromotedPieceType.P_ROOK
      case PromotablePieceType.BISHOP => PromotedPieceType.P_BISHOP
      case PromotablePieceType.LANCE => PromotedPieceType.P_LANCE
      case PromotablePieceType.KNIGHT => PromotedPieceType.P_KNIGHT
      case PromotablePieceType.SILVER => PromotedPieceType.P_SILVER
      case PromotablePieceType.PAWN => PromotedPieceType.P_PAWN
    }
  }
}

object PromotedPieceType {
  def demote(piece: PromotedPieceType): PromotablePieceType = {
    piece match {
      case PromotedPieceType.P_ROOK => PromotablePieceType.ROOK
      case PromotedPieceType.P_BISHOP => PromotablePieceType.BISHOP
      case PromotedPieceType.P_LANCE => PromotablePieceType.LANCE
      case PromotedPieceType.P_KNIGHT => PromotablePieceType.KNIGHT
      case PromotedPieceType.P_SILVER => PromotablePieceType.SILVER
      case PromotedPieceType.P_PAWN => PromotablePieceType.PAWN
    }
  }
}
