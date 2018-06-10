package com.github.j5ik2o.bacs.model

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

import scala.concurrent.duration._

case class ChildOrderRequest(productCode: ProductCode,
                             childOrderType: ChildOrderType,
                             side: Side,
                             price: Option[BigDecimal],
                             size: BigDecimal,
                             minuteToExpire: Duration = 30 days,
                             timeInForce: TimeInForce = TimeInForce.GTC) {
  require(childOrderType == ChildOrderType.LIMIT && price.isEmpty)
}

object ChildOrderRequest extends JsonImplicits {

  implicit val ChildOrderEncoder: Encoder[ChildOrderRequest] =
    Encoder.instance { v =>
      Json.obj(
        "product_code" -> v.productCode.asJson,
        "child_order_type" -> v.childOrderType.asJson,
        "side" -> v.side.asJson,
        "price" -> v.price.asJson,
        "size" -> v.size.asJson,
        "minute_to_expire" -> v.minuteToExpire.toMinutes.asJson,
        "time_in_force" -> v.timeInForce.asJson
      )
    }

  implicit val ChildOrderDecoder: Decoder[ChildOrderRequest] =
    Decoder.instance { hcursor =>
      for {
        productCode <- hcursor.get[ProductCode]("product_code")
        childOrderType <- hcursor.get[ChildOrderType]("child_order_type")
        side <- hcursor.get[Side]("side")
        price <- hcursor.get[Option[BigDecimal]]("price")
        size <- hcursor.get[BigDecimal]("size")
        minuteToExpire <- hcursor.get[Long]("minute_to_expire").map(_ minutes)
        timeInForce <- hcursor.get[TimeInForce]("time_in_force")
      } yield
        ChildOrderRequest(
          productCode,
          childOrderType,
          side,
          price,
          size,
          minuteToExpire,
          timeInForce
        )
    }

}
