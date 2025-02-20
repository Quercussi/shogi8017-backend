package com.shogi8017.app.middlewares.middlewareUtils

import cats.data.{Kleisli, OptionT}
import cats.effect.IO
import cats.syntax.applicative.*
import org.http4s.{AuthedRoutes, Response, Status}

trait CommonAuthHandlers {
  protected val onFailure: AuthedRoutes[String, IO] = Kleisli { e =>
    OptionT.liftF {
      Response[IO](status = Status.Forbidden).withEntity(e.context).pure[IO]
    }
  }
}
