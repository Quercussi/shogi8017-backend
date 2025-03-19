package com.shogi8017.app.models.enumerators

import doobie.util.{Get, Put}

enum ActionType:
  case MOVE, DROP, RESIGN
  
object ActionType:
  implicit val actionTypeGet: Get[ActionType] = Get[String].temap {
    case "MOVE" => Right(ActionType.MOVE)
    case "DROP" => Right(ActionType.DROP)
    case "RESIGN" => Right(ActionType.RESIGN)
    case other => Left(s"Unknown action: $other")
  }
  
  implicit val actionTypePut: Put[ActionType] = Put[String].contramap(_.toString)