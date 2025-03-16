package com.shogi8017.app.repository

import com.shogi8017.app.models.UserModel
import com.shogi8017.app.routes.{GetUserByIdPayload, PaginatedSearchUserPayload, PaginatedSearchUserResponse}

case class CreateUserPayload(username: String, hashedPassword: String)
case class GetUserByIdPayloadRepo(userId: String)
case class FindUserByUsernamePayload(username: String)
case class PaginatedSearchUserPayloadRepo(searchQuery: String, offset: Int, limit: Int, excludingUserIds: List[String])
case class PaginatedSearchUserResponseRepo(users: List[UserModel], nextOffset: Int, total: Int)

object PaginatedSearchUserPayloadRepo {
  def fromPaginatedSearchUserPayload(payload: PaginatedSearchUserPayload): PaginatedSearchUserPayloadRepo = {
    PaginatedSearchUserPayloadRepo(payload.searchQuery, payload.offset, payload.limit, payload.excludingUserIds)
  }
}

object PaginatedSearchUserResponseRepo {
  def toPaginatedSearchUserResponse(payload: PaginatedSearchUserResponseRepo): PaginatedSearchUserResponse = {
    PaginatedSearchUserResponse(payload.users, payload.users.length, payload.nextOffset, payload.total)
  }
}

object GetUserByIdPayloadRepo {
  def fromGetUserByIdPayload(payload: GetUserByIdPayload): GetUserByIdPayloadRepo = {
    GetUserByIdPayloadRepo(payload.userId)
  }
}