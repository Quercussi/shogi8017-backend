package com.shogi8017.command.migration

import cats.data.EitherT
import cats.effect.{ExitCode, IO, Resource}
import com.shogi8017.app.{AppConfig, DatabaseConfig}
import com.typesafe.scalalogging.LazyLogging
import fly4s.Fly4s
import fly4s.data.{Fly4sConfig, Locations}

object DatabaseMigrations extends LazyLogging {
  
  def logDatabaseConfig(dbConfig: DatabaseConfig): IO[Unit] =
    IO(logger.info(s"\nLoaded database configuration: ${dbConfig.prettyPrint}"))

  private def logValidationErrors(error: Throwable): IO[Unit] =
    IO(logger.error(s"\nMigration failed due to error: ${error.getMessage}", error))

  def createFly4sResource(dbConfig: DatabaseConfig): Resource[IO, Fly4s[IO]] =
    Fly4s.make[IO](
      url = dbConfig.connectionUrl,
      user = Some(dbConfig.user),
      password = Some(dbConfig.password.toCharArray),
      config = Fly4sConfig.default
        .withGroup(true)
        .withOutOfOrder(false)
        .withTable(dbConfig.migrationsTable)
        .withLocations(Locations(dbConfig.migrationsLocations))
        .withBaselineOnMigrate(true)
    )
  
  def handleMigration(fly4s: Fly4s[IO]): IO[ExitCode] =
    fly4s.migrate.attempt.flatMap {
      case Right(_) => IO.pure(ExitCode.Success)
      case Left(error) => logValidationErrors(error).as(ExitCode.Error)
    }

  def loadAppConfig(configNamespace: String): EitherT[IO, Throwable, AppConfig] = AppConfig.loadConfig(configNamespace)
}
