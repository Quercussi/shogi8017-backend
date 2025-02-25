package com.shogi8017.app.routes

import com.shogi8017.app.models.UserModel
import org.http4s.dsl.io.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

case class PaginatedSearchUserPayload(searchQuery: String, offset: Int, limit: Int, excludingUserIds: List[String])
case class PaginatedSearchUserResponse(users: List[UserModel], count: Int, nextOffset: Int, total: Int)

object SearchQueryParam extends QueryParamDecoderMatcher[String]("searchQuery")
object OffsetParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
object ExcludeRequestingUserParam extends OptionalQueryParamDecoderMatcher[Boolean]("excludeRequestingUser")