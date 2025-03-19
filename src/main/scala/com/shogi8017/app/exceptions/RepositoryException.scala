package com.shogi8017.app.exceptions

case class RepositoryException(cause: Throwable) extends AppException
