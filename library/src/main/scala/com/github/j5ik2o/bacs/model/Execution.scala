package com.github.j5ik2o.bacs.model

import java.time.ZonedDateTime

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Execution(id: Long,
                     side: Side,
                     price: BigDecimal,
                     size: BigDecimal,
                     execDate: ZonedDateTime,
                     buyChildOrderAcceptanceId: String,
                     sellChildOrderAcceptanceId: String)

object Execution extends JsonImplicits {

  implicit val ExecutionEncoder: Encoder[Execution] = Encoder.instance { v =>
    Json.obj(
      "id" -> v.id.asJson,
      "side" -> v.side.asJson,
      "price" -> v.price.asJson,
      "size" -> v.size.asJson,
      "exec_date" -> v.execDate.asJson,
      "buy_child_order_acceptance_id" -> v.buyChildOrderAcceptanceId.asJson,
      "sell_child_order_acceptance_id" -> v.sellChildOrderAcceptanceId.asJson
    )
  }

  implicit val ExecutionDecoder: Decoder[Execution] = Decoder.instance {
    hcursor =>
      for {
        id <- hcursor.get[Long]("id")
        side <- hcursor.get[Side]("side")
        price <- hcursor.get[BigDecimal]("price")
        size <- hcursor.get[BigDecimal]("size")
        execDate <- hcursor.get[ZonedDateTime]("exec_date")
        buyChildOrderAcceptanceId <- hcursor.get[String](
          "buy_child_order_acceptance_id")
        sellChildOrderAcceptanceId <- hcursor.get[String](
          "sell_child_order_acceptance_id")
      } yield
        Execution(id,
                  side,
                  price,
                  size,
                  execDate,
                  buyChildOrderAcceptanceId,
                  sellChildOrderAcceptanceId)
  }
}
