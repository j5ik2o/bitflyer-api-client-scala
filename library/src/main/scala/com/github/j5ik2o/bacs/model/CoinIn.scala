package com.github.j5ik2o.bacs.model

import java.time.ZonedDateTime

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class CoinIn(id: Long,
                  orderId: String,
                  currencyCode: CurrencyCode,
                  amount: BigDecimal,
                  address: String,
                  txHash: String,
                  status: String,
                  eventDate: ZonedDateTime)

object CoinIn extends JsonImplicits {

  implicit val CoinInEncoder: Encoder[CoinIn] = Encoder.instance { v =>
    Json.obj(
      "id" -> v.id.asJson,
      "order_id" -> v.orderId.asJson,
      "currency_code" -> v.currencyCode.asJson,
      "amount" -> v.amount.asJson,
      "address" -> v.address.asJson,
      "tx_hash" -> v.txHash.asJson,
      "status" -> v.status.asJson,
      "event_date" -> v.eventDate.asJson
    )
  }

  implicit val CoinInDecoder: Decoder[CoinIn] = Decoder.instance { hcursor =>
    for {
      id <- hcursor.get[Long]("id")
      orderId <- hcursor.get[String]("order_id")
      currencyCode <- hcursor.get[CurrencyCode]("currency_code")
      amount <- hcursor.get[BigDecimal]("amount")
      address <- hcursor.get[String]("address")
      txHash <- hcursor.get[String]("tx_hash")
      status <- hcursor.get[String]("status")
      eventDate <- hcursor.get[ZonedDateTime]("event_date")
    } yield
      CoinIn(id,
             orderId,
             currencyCode,
             amount,
             address,
             txHash,
             status,
             eventDate)
  }

}
