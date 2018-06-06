package com.github.j5ik2o.bacs.model

import java.time.ZonedDateTime

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Withdrawal(id: Long,
                      orderId: String,
                      currencyCode: CurrencyCode,
                      amount: BigDecimal,
                      status: String,
                      eventDate: ZonedDateTime)

object Withdrawal extends JsonImplicits {

  implicit val WithdrawalEncoder: Encoder[Withdrawal] = Encoder.instance { v =>
    Json.obj(
      "id" -> v.id.asJson,
      "order_id" -> v.orderId.asJson,
      "currency_code" -> v.currencyCode.asJson,
      "amount" -> v.amount.asJson,
      "status" -> v.status.asJson,
      "event_date" -> v.eventDate.asJson
    )
  }

  implicit val WithdrawalDecoder: Decoder[Withdrawal] = Decoder.instance {
    hcursor =>
      for {
        id <- hcursor.get[Long]("id")
        orderId <- hcursor.get[String]("order_id")
        currencyCode <- hcursor.get[CurrencyCode]("currency_code")
        amount <- hcursor.get[BigDecimal]("amount")
        status <- hcursor.get[String]("status")
        eventDate <- hcursor.get[ZonedDateTime]("event_date")
      } yield Withdrawal(id, orderId, currencyCode, amount, status, eventDate)
  }

}
