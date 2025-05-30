package com.shogi8017.app.exceptions

sealed trait DatabaseRecordException extends AppException

sealed trait InvalidInvitation extends DatabaseRecordException
case object InvitationNotFound extends InvalidInvitation

sealed trait InvalidGame extends DatabaseRecordException
case object GameNotFound extends InvalidGame

sealed trait InvalidBoardHistory extends DatabaseRecordException
case object InvalidMoveRecord extends InvalidBoardHistory
case object InvalidDropRecord extends InvalidBoardHistory
case object InvalidResignRecord extends InvalidBoardHistory
