package com.github.j5ik2o.bacs.model

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Market(productCode: ProductCode, alias: Option[String] = None)

object Market extends JsonImplicits {

  implicit val MarketEncoder: Encoder[Market] = Encoder.instance { v =>
    Json.obj(
      "product_code" -> v.productCode.asJson,
      "alias" -> v.alias.asJson
    )
  }

  implicit val MarketDecoder: Decoder[Market] = Decoder.instance { hcursor =>
    for {
      productCode <- hcursor.get[ProductCode]("product_code")
      alias <- hcursor.get[Option[String]]("alias")
    } yield Market(productCode, alias)
  }

}
