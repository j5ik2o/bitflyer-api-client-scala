package com.github.j5ik2o.bacs.model

import enumeratum._
import io.circe.{Decoder, Encoder}

sealed trait TimeInForce extends EnumEntry

object TimeInForce extends Enum[TimeInForce] {
  override def values = findValues

  case object GTC extends TimeInForce
  case object IOC extends TimeInForce
  case object FOK extends TimeInForce

  implicit val TimeInForceEncoder: Encoder[TimeInForce] =
    Encoder[String].contramap(_.entryName)

  implicit val TimeInForceDecoder: Decoder[TimeInForce] =
    Decoder[String].map(TimeInForce.withName)
}
