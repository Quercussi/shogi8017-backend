package com.shogi8017.app.services.logics

case class Direction(dx: Int, dy: Int)

object Direction:
  def calculateDirection(from: Position, to: Position): Direction = {
    val dx = to.x - from.x
    val dy = to.y - from.y
    Direction(dx, dy)
  }