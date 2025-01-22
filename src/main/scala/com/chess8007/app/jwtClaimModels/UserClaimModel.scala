package com.chess8007.app.jwtClaimModels

import com.chess8007.app.models.UserModel
import io.circe.{Encoder, Json}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.syntax.*
import pdi.jwt.JwtClaim

case class UserClaimModel(userId: String, username: String) extends JwtClaimModel {
  def asJsonString: String = this.asJson.toString
}

object UserClaimModel {
  implicit val userClaimModelEncoder: Encoder[UserClaimModel] = deriveEncoder[UserClaimModel]

  def of(user: UserModel): UserClaimModel = {
    UserClaimModel(user.userId, user.username)
  }
}
