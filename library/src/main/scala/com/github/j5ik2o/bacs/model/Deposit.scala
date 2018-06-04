package com.github.j5ik2o.bacs.model

import java.time.ZonedDateTime

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Deposit(id: Long,
                   orderId: String,
                   currencyCode: String,
                   amount: BigDecimal,
                   status: String,
                   eventDate: ZonedDateTime)

object Deposit extends JsonImplicits {

  implicit val DepositEncoder: Encoder[Deposit] = Encoder.instance { v =>
    Json.obj(
      "id" -> v.id.asJson,
      "order_id" -> v.orderId.asJson,
      "currencyCode" -> v.currencyCode.asJson,
      "amount" -> v.amount.asJson,
      "status" -> v.status.asJson,
      "eventDate" -> v.eventDate.asJson
    )
  }

  implicit val DepositDecoder: Decoder[Deposit] = Decoder.instance { hcursor =>
    for {
      id <- hcursor.get[Long]("id")
      orderId <- hcursor.get[String]("order_id")
      currencyCode <- hcursor.get[String]("currency_code")
      amount <- hcursor.get[BigDecimal]("amount")
      status <- hcursor.get[String]("status")
      eventDate <- hcursor.get[ZonedDateTime]("event_date")
    } yield Deposit(id, orderId, currencyCode, amount, status, eventDate)

  }

}
