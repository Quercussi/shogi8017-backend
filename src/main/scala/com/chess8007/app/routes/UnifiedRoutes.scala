package com.chess8007.app.routes

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import com.chess8007.app.middlewares.*
import com.chess8007.app.repository.*
import com.chess8007.app.services.*
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2

class UnifiedRoutes(
  wbs: WebSocketBuilder2[IO],
  mc: MiddlewareCollection,
  authenticationRoutes: AuthenticationRoutes,
  unauthenticatedRoutes: UnauthenticatedRoutes,
  invitationRoutes: InvitationRoutes
) {
  private val publicRoutes = Router(
    "/api" -> (authenticationRoutes.getLoginRoute <+> unauthenticatedRoutes.getSignUpRoute)
  )

  private def wsRoutes(wbs: WebSocketBuilder2[IO]) = Router(
    "/ws" -> mc.accessTokenMiddleware(invitationRoutes.routes(wbs))
  )

  private val refreshRoutes = Router(
    "/api" -> mc.refreshTokenMiddleware(authenticationRoutes.getRefreshTokenRoute)
  )

  def getRoutes: HttpRoutes[IO] =
    publicRoutes <+> wsRoutes(wbs) <+> refreshRoutes
}

object UnifiedRoutes {

  def instantiateRoutes(
     wbs: WebSocketBuilder2[IO],
     wsBuffer: WebSocketRouteBuffer,
     middlewareCollection: MiddlewareCollection,
     serviceCollection: ServiceCollection,
  ): UnifiedRoutes = {
    val authenticationRoutes = AuthenticationRoutes.of(serviceCollection.authenticationService)
    val unauthenticatedRoutes = UnauthenticatedRoutes.of(serviceCollection.userService)
    val invitationRoutes = InvitationRoutes.of(wsBuffer.invitationRouteBuffer)
    new UnifiedRoutes(
      wbs,
      middlewareCollection,
      authenticationRoutes,
      unauthenticatedRoutes,
      invitationRoutes
    )
  }

  def initializeWebSocketProcessingStream(
    wsBuffer: WebSocketRouteBuffer,
    repositoryCollection: RepositoryCollection
  ): IO[Unit] = {
    val invitationRouteBuffer = wsBuffer.invitationRouteBuffer
    val invitationService = new InvitationService(repositoryCollection.gameRepository)
    invitationService
      .initiateProcessingStream(invitationRouteBuffer.processQueue, invitationRouteBuffer.clientRegistry)
      .compile
      .drain
      .handleErrorWith(err => IO(println(s"Error in processing stream: ${err.getMessage}")))
      .start
      .void
  }

}