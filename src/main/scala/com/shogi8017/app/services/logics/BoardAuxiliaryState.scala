package com.shogi8017.app.services.logics

import com.shogi8017.app.models.enumerators.GameWinner

case class BoardAuxiliaryState(lastAction: Option[Actor], gameWinner: Option[GameWinner])
