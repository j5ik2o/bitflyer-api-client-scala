package com.github.j5ik2o.bacs.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Bid(price: BigDecimal, size: BigDecimal)
case class Ask(price: BigDecimal, size: BigDecimal)

case class BoardResponse(midPrice: Long, bids: List[Bid], asks: List[Ask])

object BoardResponse {
  import io.circe.generic.auto._
  implicit val BoardResponseEncoder: Encoder[BoardResponse] = Encoder.instance {
    v =>
      Json.obj(
        "mid_price" -> v.midPrice.asJson,
        "bids" -> v.bids.asJson,
        "asks" -> v.asks.asJson
      )
  }

  implicit val BoardResponseDecoder: Decoder[BoardResponse] = Decoder.instance {
    hcursor =>
      for {
        midPrice <- hcursor.get[Long]("mid_price")
        bids <- hcursor.get[List[Bid]]("bids")
        asks <- hcursor.get[List[Ask]]("asks")
      } yield BoardResponse(midPrice, bids, asks)
  }

}
