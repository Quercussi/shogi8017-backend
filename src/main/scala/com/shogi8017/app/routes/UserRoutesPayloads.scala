package com.shogi8017.app.routes

import com.shogi8017.app.models.UserModel

case class PaginatedSearchUserPayload(searchQuery: String, offset: Int, limit: Int, excludingUserIds: List[String])
case class PaginatedSearchUserResponse(users: List[UserModel], count: Int, nextOffset: Int, total: Int)

case class GetUserByIdPayload(userId: String)
case class GetUserByIdResponse(user: Option[UserModel])