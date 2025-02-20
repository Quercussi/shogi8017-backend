package com.shogi8017.app.middlewares

import com.shogi8017.app.jwtClaimModels.UserClaimModel
import com.shogi8017.app.models.UserModel

trait UserTokenMiddleware extends TokenMiddleware[UserClaimModel, UserModel] {
  override def classMapper: UserClaimModel => UserModel = { userClaimModel =>
    UserModel(
      userId = userClaimModel.userId,
      username = userClaimModel.username
    )
  }
}
