package com.shogi8017.app.routes

import org.http4s.dsl.io.{OptionalQueryParamDecoderMatcher, QueryParamDecoderMatcher}

object SearchQueryParam extends QueryParamDecoderMatcher[String]("searchQuery")
object OffsetParam extends OptionalQueryParamDecoderMatcher[Int]("offset")
object LimitParam extends OptionalQueryParamDecoderMatcher[Int]("limit")
object ExcludeRequestingUserParam extends OptionalQueryParamDecoderMatcher[Boolean]("excludeRequestingUser")