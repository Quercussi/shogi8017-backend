package com.shogi8017.app.services.logics.pieces

import com.shogi8017.app.services.logics.Player

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
