package com.github.j5ik2o.bacs.model

import enumeratum._
import io.circe.{Decoder, Encoder}

sealed trait CurrencyCode extends EnumEntry

object CurrencyCode extends Enum[CurrencyCode] {
  override def values = findValues

  case object JPY extends CurrencyCode
  case object BTC extends CurrencyCode
  case object BCH extends CurrencyCode
  case object ETH extends CurrencyCode
  case object ETC extends CurrencyCode
  case object LTC extends CurrencyCode
  case object MONA extends CurrencyCode
  case object LSK extends CurrencyCode

  implicit val CurrencyCodeEncoder: Encoder[CurrencyCode] =
    Encoder[String].contramap(_.entryName)

  implicit val CurrencyCodeDecoder: Decoder[CurrencyCode] =
    Decoder[String].map(CurrencyCode.withName)

}
