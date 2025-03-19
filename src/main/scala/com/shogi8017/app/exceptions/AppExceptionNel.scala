package com.shogi8017.app.exceptions

import cats.data.NonEmptyList

case class AppExceptionNel(exceptions: NonEmptyList[AppException]) extends AppException {
  override def getMessage: String =
    exceptions.toList.map(_.toString).mkString("Multiple exceptions occurred:\n - ", "\n - ", "")

  override def toString: String = getMessage
}

object AppExceptionNel {
  def singleton(exception: AppException): AppExceptionNel = AppExceptionNel(NonEmptyList.one(exception))

  def push(exception: AppException): AppExceptionNel => AppExceptionNel = 
    (appExceptionNel: AppExceptionNel) => AppExceptionNel(appExceptionNel.exceptions :+ exception)

  def apply(exceptions: NonEmptyList[AppException]): AppExceptionNel = new AppExceptionNel(exceptions)
}