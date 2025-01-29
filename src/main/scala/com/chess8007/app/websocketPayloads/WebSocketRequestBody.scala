package com.chess8007.app.websocketPayloads

import io.circe.{ACursor, Decoder, DecodingFailure}


trait WebSocketRequestBody

object WebSocketRequestBody {

  /**
   * A helper method that decodes a `WebSocketRequestBody` from a `cursor` based on the provided action handlers.
   *
   * @param cursor         The `ACursor` to read the JSON data from.
   * @param actionHandlers A `PartialFunction` that maps an action name (a `String`) to a specific decoder for the corresponding `WebSocketRequestBody` type.
   * @tparam T The type of the `WebSocketRequestBody` to decode.
   * @return A `Decoder.Result[T]` which is either:
   *         - `Right(decodedValue)` if decoding is successful, or
   *         - `Left(DecodingFailure)` if decoding fails or the action is unsupported.
   *
   * This method first extracts the "action" field from the JSON object. It then uses the `actionHandlers`
   *         to determine which decoder to use based on the value of the action. If the action is supported, it decodes 
   *         the "payload" field using the selected decoder. If the action is not supported, it returns a `DecodingFailure`.
   */
  def dispatchDecoder[T <: WebSocketRequestBody](
    cursor: ACursor,
    actionHandlers: PartialFunction[String, Decoder[T]]
  ): Decoder.Result[T] = {
    cursor.get[String]("action").flatMap { action =>
      actionHandlers.lift(action) match {
        case Some(decoder) =>
          cursor.downField("payload").as(decoder)
        case None =>
          Left(DecodingFailure(s"Unsupported action: $action", cursor.history))
      }
    }
  }
}