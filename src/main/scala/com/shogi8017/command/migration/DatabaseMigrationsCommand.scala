package com.shogi8017.command.migration

import cats.effect.{ExitCode, IO, IOApp}
import com.shogi8017.app.AppConfig
import com.shogi8017.command.migration.DatabaseMigrations.{createFly4sResource, handleMigration, loadAppConfig, logDatabaseConfig, logger}
import com.typesafe.scalalogging.LazyLogging

object DatabaseMigrationsCommand extends IOApp with LazyLogging {
  private val defaultConfigNamespace = "shogi8017"
  
  def run(args: List[String]): IO[ExitCode] = {
    val configNamespace = args.headOption.getOrElse(defaultConfigNamespace)
    
    for {
      appConfigEither <- AppConfig.loadConfig(configNamespace).value
      result <- appConfigEither match {
        case Right(appConfig) =>
          val dbConfig = appConfig.database
          for {
            _ <- logDatabaseConfig(dbConfig)
            fly4sResource = createFly4sResource(dbConfig)
            exitCode <- fly4sResource.use(handleMigration)
          } yield exitCode

        case Left(error) =>
          IO {
            logger.error(s"\nFailed to load configuration: ${error.getMessage}")
            ExitCode.Error
          }
      }
    } yield result
  }
}