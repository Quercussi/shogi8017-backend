package com.shogi8017.app.routes

import cats.effect.IO
import cats.implicits.toSemigroupKOps
import com.shogi8017.app.middlewares.*
import com.shogi8017.app.repository.*
import com.shogi8017.app.services.*
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.server.websocket.WebSocketBuilder2

class UnifiedRoutes(
  wbs: WebSocketBuilder2[IO],
  mc: MiddlewareCollection,
  authenticationRoutes: AuthenticationRoutes,
  unauthenticatedRoutes: UnauthenticatedRoutes,
  invitationRoutes: InvitationRoutes,
  gameActionRoutes: GameActionRoutes,
) {
  private val publicRoutes = Router(
    "/api" -> (authenticationRoutes.getLoginRoute <+> unauthenticatedRoutes.getSignUpRoute)
  )

  private def wsRoutes(wbs: WebSocketBuilder2[IO]) = Router(
    "/ws" -> mc.accessTokenMiddleware(gameActionRoutes.routes(wbs) <+> invitationRoutes.routes(wbs))
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
    val gameActionRoutes = GameActionRoutes.of(wsBuffer.gameActionRouteBuffer)
    new UnifiedRoutes(
      wbs,
      middlewareCollection,
      authenticationRoutes,
      unauthenticatedRoutes,
      invitationRoutes,
      gameActionRoutes
    )
  }

  def initializeWebSocketProcessingStream(
    wsBuffer: WebSocketRouteBuffer,
    repositoryCollection: RepositoryCollection
  ): IO[Unit] = {
    val invitationStream = {
      val invitationRouteBuffer = wsBuffer.invitationRouteBuffer
      val invitationService = new InvitationService(repositoryCollection.invitationRepository)
      invitationService
        .initiateProcessingStream(invitationRouteBuffer.processQueue, invitationRouteBuffer.clientRegistry)
        .compile
        .drain
        .handleErrorWith(err => IO(println(s"Error in invitation stream: ${err.getMessage}")))
        .start
        .void
    }

    val gameActionStream = {
      val gameActionRouteBuffer = wsBuffer.gameActionRouteBuffer
      val gameActionService = new GameActionService(
        repositoryCollection.gameRepository,
        repositoryCollection.invitationRepository,
        repositoryCollection.boardHistoryRepository,
        repositoryCollection.userRepository
      )
      gameActionService
        .initiateProcessingStream(gameActionRouteBuffer.processQueue, gameActionRouteBuffer.clientRegistry)
        .compile
        .drain
        .handleErrorWith(err => IO(println(s"Error in game action stream: ${err.getMessage}")))
        .start
        .void
    }

    gameActionStream *> invitationStream  
  }

}