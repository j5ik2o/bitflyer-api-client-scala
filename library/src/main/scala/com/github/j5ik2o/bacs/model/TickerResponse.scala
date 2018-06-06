package com.github.j5ik2o.bacs.model

import java.time.ZonedDateTime

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe._
import io.circe.syntax._

case class TickerResponse(product_code: String,
                          timestamp: ZonedDateTime,
                          tickId: Long,
                          bestBid: BigDecimal,
                          bestAsk: BigDecimal,
                          bestBidSize: BigDecimal,
                          bestAskSize: BigDecimal,
                          totalBidDepth: BigDecimal,
                          totalAskDepth: BigDecimal,
                          ltp: BigDecimal,
                          volume: BigDecimal,
                          volumeByProduct: BigDecimal)

object TickerResponse extends JsonImplicits {

  implicit val TickerEncoder: Encoder[TickerResponse] =
    Encoder.instance[TickerResponse] { v =>
      Json.obj(
        "product_code" -> v.product_code.asJson,
        "timestamp" -> v.timestamp.asJson,
        "tick_id" -> v.tickId.asJson,
        "best_bid" -> v.bestBid.asJson,
        "best_ask" -> v.bestAsk.asJson,
        "best_bid_size" -> v.bestBidSize.asJson,
        "best_ask_size" -> v.bestAskSize.asJson,
        "total_bid_depth" -> v.totalBidDepth.asJson,
        "total_ask_depth" -> v.totalAskDepth.asJson,
        "ltp" -> v.ltp.asJson,
        "volume" -> v.volume.asJson,
        "volume_by_product" -> v.volumeByProduct.asJson
      )
    }

  implicit val TickerDecoder: Decoder[TickerResponse] = Decoder.instance {
    hcursor =>
      for {
        productCode <- hcursor.get[String]("product_code")
        timestamp <- hcursor.get[ZonedDateTime]("timestamp")
        tickId <- hcursor.get[Long]("tick_id")
        bestBid <- hcursor.get[BigDecimal]("best_bid")
        bestAsk <- hcursor.get[BigDecimal]("best_ask")
        bestBidSize <- hcursor.get[BigDecimal]("best_bid_size")
        bestAskSize <- hcursor.get[BigDecimal]("best_ask_size")
        totalBidDepth <- hcursor.get[BigDecimal]("total_bid_depth")
        totalAskDepth <- hcursor.get[BigDecimal]("total_ask_depth")
        ltp <- hcursor.get[BigDecimal]("ltp")
        volume <- hcursor.get[BigDecimal]("volume")
        volumeByProduct <- hcursor.get[BigDecimal]("volume_by_product")
      } yield
        TickerResponse(productCode,
                       timestamp,
                       tickId,
                       bestBid,
                       bestAsk,
                       bestBidSize,
                       bestAskSize,
                       totalBidDepth,
                       totalAskDepth,
                       ltp,
                       volume,
                       volumeByProduct)

  }

}
