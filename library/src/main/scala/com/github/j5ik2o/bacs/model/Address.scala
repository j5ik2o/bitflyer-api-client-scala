package com.github.j5ik2o.bacs.model

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Address(`type`: String, currencyCode: CurrencyCode, address: String)

object Address extends JsonImplicits {

  implicit val AddressEncoder: Encoder[Address] = Encoder.instance { v =>
    Json.obj(
      "type" -> v.`type`.asJson,
      "currency_code" -> v.currencyCode.asJson,
      "address" -> v.address.asJson
    )
  }

  implicit val AddressDecoder: Decoder[Address] = Decoder.instance { hcursor =>
    for {
      t <- hcursor.get[String]("type")
      currencyCode <- hcursor.get[CurrencyCode]("currency_code")
      address <- hcursor.get[String]("address")
    } yield Address(t, currencyCode, address)
  }

}
