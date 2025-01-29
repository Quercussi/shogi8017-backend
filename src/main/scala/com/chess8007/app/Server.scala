package com.chess8007.app

import cats.effect.{IO, Resource}
import cats.implicits.toSemigroupKOps
import com.chess8007.app.database.Database
import com.chess8007.app.middlewares.MiddlewareCollection
import com.chess8007.app.repository.RepositoryCollection
import com.chess8007.app.routes.UnifiedRoutes.initializeWebSocketProcessingStream
import com.chess8007.app.routes.{UnifiedRoutes, WebSocketRouteBuffer}
import com.chess8007.app.services.ServiceCollection
import com.comcast.ip4s.*
import doobie.hikari.HikariTransactor
import org.http4s.HttpRoutes
import org.http4s.dsl.io.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server

object Server {
  private val healthRoute: HttpRoutes[IO] = HttpRoutes.of {
    case GET -> Root / "api" / "health" =>
      Ok("OK")
  }
  
  val configNamespace = "chess8007"
  
  def server: Resource[IO, Server] = for {
    _ <- Resource.eval(IO.println("Loading app configuration"))
    appConfig <- Resource.eval(AppConfig.loadConfig(configNamespace).flatMap {
      case Right(config) => IO.pure(config)
      case Left(error)   => IO.raiseError(error)
    })

    trx: HikariTransactor[IO] <- Database.transactor(appConfig)

    repoCollection <- Resource.eval(IO(RepositoryCollection.instantiateRepository(trx)))

    serviceCollection <- Resource.eval(IO(ServiceCollection.instantiateServices(appConfig, repoCollection)))

    middlewareCollection <- Resource.eval(IO(MiddlewareCollection.instantiateMiddlewares(appConfig.jwt)))

    wsBuffer <- WebSocketRouteBuffer.resource

    _ <- Resource.eval(initializeWebSocketProcessingStream(wsBuffer, repoCollection))

    server <- EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpWebSocketApp { wsb =>
        (healthRoute <+>
          UnifiedRoutes.instantiateRoutes(wsb, wsBuffer, middlewareCollection, serviceCollection).getRoutes)
          .orNotFound
      }
      .build

  } yield server
}