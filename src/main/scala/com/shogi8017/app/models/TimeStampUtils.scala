package com.shogi8017.app.models

import io.circe.Decoder.Result
import io.circe.{Decoder, Encoder, HCursor, Json}

import java.sql.Timestamp

object TimeStampUtils {
  implicit val timestampEncoder: Encoder[Timestamp] = Encoder.instance { ts =>
    Json.fromLong(ts.getTime)
  }

  implicit val timestampDecoder: Decoder[Timestamp] = Decoder.instance { cursor =>
    cursor.as[Long].map(ms => new Timestamp(ms))
  }
}
