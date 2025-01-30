package com.shogi8017.app

import cats.effect.{ExitCode, IO, IOApp}

object Application extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    Server.server
      .use(_ => IO.never)
      .as(ExitCode.Success)
}