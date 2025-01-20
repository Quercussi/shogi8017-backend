package com.chess8007.app

import com.chess8007.app.models.UserModel
import com.comcast.ip4s.*
import cats.effect.{IO, Resource}
import cats.implicits.toSemigroupKOps
import com.chess8007.app.routes.UnifiedRoutes
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

object Server {

//  private def routes(ws: WebSocketBuilder2[IO]): HttpRoutes[IO] =
//    HttpRoutes.of {
//      case GET -> Root / "ws" =>
//        val send: Stream[IO, WebSocketFrame] =
//          Stream.awakeEvery[IO](1.second)
//            .evalMap(_ => IO(WebSocketFrame.Text("ok")))
//        val receive: Pipe[IO, WebSocketFrame, Unit] =
//          in => in.evalMap(frameIn => IO(println("in " + frameIn.length)))
//
//        ws.build(send, receive)
//    }

    private val healthRoute: HttpRoutes[IO] = HttpRoutes.of {
      case GET -> Root / "health" =>
        Ok("OK")
    }

//  private val authenticatedRoutes: AuthedRoutes[UserModel,IO] = AuthedRoutes.of {
//      case GET -> Root / "welcome" as user =>
//        Ok(s"Welcome, ${user.username}")
//    }


  val configNamespace = "chess8007"

  def server: Resource[IO, Server] = for {
    // Variable set-up
    appConfig <- Resource.eval(AppConfig.loadConfig(configNamespace).flatMap {
      case Right(config) => IO.pure(config)
      case Left(error) => IO.raiseError(error)
    })
//    jwtAuthMiddleware = BasicAuthMiddleware.of(appConfig.jwt)

    // Server instantiation
    server <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp((healthRoute <+> UnifiedRoutes.of(appConfig).getRoutes).orNotFound)
//    .withHttpApp(jwtAuthMiddleware.wrap(authenticatedRoutes).orNotFound)
//    .withHttpApp(jwtAuthMiddleware(authenticatedRoutes).orNotFound)
      .build
//    .useForever
//    .void
  } yield server
}