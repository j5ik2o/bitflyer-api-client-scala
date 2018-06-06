package com.github.j5ik2o.bacs.model

import java.time.ZonedDateTime

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class CoinOut(id: Long,
                   orderId: String,
                   currencyCode: CurrencyCode,
                   amount: BigDecimal,
                   address: String,
                   txHash: String,
                   fee: BigDecimal,
                   additionalFee: BigDecimal,
                   status: String,
                   eventDate: ZonedDateTime)

object CoinOut extends JsonImplicits {

  implicit val CoinOutEncoder: Encoder[CoinOut] = Encoder.instance { v =>
    Json.obj(
      "id" -> v.id.asJson,
      "order_id" -> v.orderId.asJson,
      "currency_code" -> v.currencyCode.asJson,
      "amount" -> v.amount.asJson,
      "address" -> v.address.asJson,
      "tx_hash" -> v.txHash.asJson,
      "fee" -> v.fee.asJson,
      "additional_fee" -> v.additionalFee.asJson,
      "status" -> v.status.asJson,
      "event_date" -> v.eventDate.asJson
    )
  }

  implicit val CoinOutDecoder: Decoder[CoinOut] = Decoder.instance { hcursor =>
    for {
      id <- hcursor.get[Long]("id")
      orderId <- hcursor.get[String]("order_id")
      currencyCode <- hcursor.get[CurrencyCode]("currency_code")
      amount <- hcursor.get[BigDecimal]("amount")
      address <- hcursor.get[String]("address")
      txHash <- hcursor.get[String]("tx_hash")
      fee <- hcursor.get[BigDecimal]("fee")
      additionalFee <- hcursor.get[BigDecimal]("additional_fee")
      status <- hcursor.get[String]("status")
      eventDate <- hcursor.get[ZonedDateTime]("event_date")
    } yield
      CoinOut(id,
              orderId,
              currencyCode,
              amount,
              address,
              txHash,
              fee,
              additionalFee,
              status,
              eventDate)
  }

}
