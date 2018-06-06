package com.github.j5ik2o.bacs.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class BoardStateData(specialQuotation: BigDecimal)

object BoardStateData {

  implicit val BoardStateDataEncoder: Encoder[BoardStateData] =
    Encoder.instance { v =>
      Json.obj(
        "special_quotation" -> v.specialQuotation.asJson
      )
    }

  implicit val BoardStateDataDecoder: Decoder[BoardStateData] =
    Decoder.instance { hcursor =>
      for {
        specialQuotation <- hcursor.get[BigDecimal]("special_quotation")
      } yield BoardStateData(specialQuotation)
    }

}

case class BoardStateResponse(health: String,
                              state: String,
                              data: Option[BoardStateData])
