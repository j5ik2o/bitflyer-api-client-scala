package com.github.j5ik2o.bacs.model

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Balance(currencyCode: CurrencyCode,
                   amount: BigDecimal,
                   available: BigDecimal)

object Balance extends JsonImplicits {

  implicit val BalanceEncoder: Encoder[Balance] = Encoder.instance { v =>
    Json.obj(
      "currency_code" -> v.currencyCode.asJson,
      "amount" -> v.amount.asJson,
      "available" -> v.available.asJson
    )
  }

  implicit val BalanceDecoder: Decoder[Balance] = Decoder.instance { hcursor =>
    for {
      currencyCode <- hcursor.get[CurrencyCode]("currency_code")
      amount <- hcursor.get[BigDecimal]("amount")
      available <- hcursor.get[BigDecimal]("available")
    } yield Balance(currencyCode, amount, available)
  }

}
