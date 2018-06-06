package com.github.j5ik2o.bacs.model

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class CollateralAccount(currencyCode: CurrencyCode, amount: BigDecimal)

object CollateralAccount extends JsonImplicits {

  implicit val CollateralAccountEncoder: Encoder[CollateralAccount] =
    Encoder.instance { v =>
      Json.obj(
        "currency_code" -> v.currencyCode.asJson,
        "amount" -> v.amount.asJson
      )
    }

  implicit val CollateralAccountDecoder: Decoder[CollateralAccount] =
    Decoder.instance { hcursor =>
      for {
        currencyCode <- hcursor.get[CurrencyCode]("currency_code")
        amount <- hcursor.get[BigDecimal]("amount")
      } yield CollateralAccount(currencyCode, amount)
    }

}
