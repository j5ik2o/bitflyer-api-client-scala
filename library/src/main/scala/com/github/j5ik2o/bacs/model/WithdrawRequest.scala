package com.github.j5ik2o.bacs.model

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class WithdrawRequest(currencyCode: CurrencyCode,
                           bankAccountId: Long,
                           amount: BigDecimal,
                           code: String)

object WithdrawRequest extends JsonImplicits {

  implicit val WithdrawRequestEncoder: Encoder[WithdrawRequest] =
    Encoder.instance { v =>
      Json.obj(
        "currency_code" -> v.currencyCode.asJson,
        "bank_account_id" -> v.bankAccountId.asJson,
        "amount" -> v.amount.asJson,
        "code" -> v.code.asJson
      )
    }

  implicit val WithdrawRequestDecoder: Decoder[WithdrawRequest] =
    Decoder.instance { hcursor =>
      for {
        currencyCode <- hcursor.get[CurrencyCode]("currency_code")
        bankAccountId <- hcursor.get[Long]("bank_account_id")
        amount <- hcursor.get[BigDecimal]("amount")
        code <- hcursor.get[String]("code")
      } yield WithdrawRequest(currencyCode, bankAccountId, amount, code)
    }

}
