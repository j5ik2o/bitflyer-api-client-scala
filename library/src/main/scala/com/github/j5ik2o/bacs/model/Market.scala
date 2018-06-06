package com.github.j5ik2o.bacs.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Market(productCode: String, alias: Option[String] = None)

object Market {

  implicit val MarketEncoder: Encoder[Market] = Encoder.instance { v =>
    Json.obj(
      "product_code" -> v.productCode.asJson,
      "alias" -> v.alias.asJson
    )
  }

  implicit val MarketDecoder: Decoder[Market] = Decoder.instance { hcursor =>
    for {
      productCode <- hcursor.get[String]("product_code")
      alias <- hcursor.get[Option[String]]("alias")
    } yield Market(productCode, alias)
  }

}
