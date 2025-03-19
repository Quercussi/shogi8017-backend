package com.shogi8017.app.jwtClaimModels

import pdi.jwt.JwtClaim

trait JwtClaimModel {
  def asJsonString: String
  def asJwtClaim(ttlSeconds: Int): JwtClaim = JwtClaim(
    content = asJsonString,
    expiration = Some(System.currentTimeMillis() / 1000 + ttlSeconds),
    issuedAt = Some(System.currentTimeMillis() / 1000),
  )
}
