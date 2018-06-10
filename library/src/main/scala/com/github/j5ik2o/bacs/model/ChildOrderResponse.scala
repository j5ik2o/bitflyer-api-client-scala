package com.github.j5ik2o.bacs.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class ChildOrderResponse(childOrderAcceptanceId: String)

object ChildOrderResponse {

  implicit val ChildOrderResponseEncoder: Encoder[ChildOrderResponse] =
    Encoder.instance { v =>
      Json.obj(
        "child_order_acceptance_id" -> v.childOrderAcceptanceId.asJson
      )
    }

  implicit val ChildOrderResponseDecoder: Decoder[ChildOrderResponse] =
    Decoder.instance { hcursor =>
      for {
        childOrderAcceptanceId <- hcursor.get[String](
          "child_order_acceptance_id")
      } yield ChildOrderResponse(childOrderAcceptanceId)
    }

}
