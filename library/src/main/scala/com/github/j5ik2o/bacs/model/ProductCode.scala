package com.github.j5ik2o.bacs.model

import enumeratum._
import io.circe.{Decoder, Encoder}

sealed trait ProductCode extends EnumEntry

object ProductCode extends Enum[ProductCode] {
  override def values = findValues

  case object BTC_JPY extends ProductCode
  case object FX_BTC_JPY extends ProductCode
  case object ETH_BTC extends ProductCode
  case object BCH_BTC extends ProductCode
  case object BTCJPY29JUN2018 extends ProductCode
  case object BTCJPY08JUN2018 extends ProductCode
  case object BTCJPY15JUN2018 extends ProductCode

  implicit val ProductCodeEncoder: Encoder[ProductCode] =
    Encoder[String].contramap(_.entryName)

  implicit val ProductCodeDecoder: Decoder[ProductCode] =
    Decoder[String].map(ProductCode.withName)
}
