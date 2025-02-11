package com.shogi8017.app.services.logics

type MoveResult = (Board, StateTransitionList, AlgebraicNotation, Option[GameEvent])
type BoardStateTransition = (Board, StateTransitionList, AlgebraicNotation)
type BoardTransition = (StateTransitionList, AlgebraicNotation)
type StateTransitionList = List[StateTransition]
type AlgebraicNotation = String