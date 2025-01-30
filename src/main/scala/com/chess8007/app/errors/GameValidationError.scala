package com.shogi8017.app.errors

sealed trait GameValidationError

sealed trait MoveValidationError extends GameValidationError
case object IllegalMove extends MoveValidationError
case object UnoccupiedPosition extends MoveValidationError
case object NoMove extends MoveValidationError
case object NoPromotion extends MoveValidationError
case object OutOfTurn extends MoveValidationError
case object OutOfBoard extends MoveValidationError

sealed trait GameStateError extends GameValidationError
case object NoKingError extends GameStateError
