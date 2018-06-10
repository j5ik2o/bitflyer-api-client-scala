package com.github.j5ik2o.bacs.model

import enumeratum.{Enum, EnumEntry}
import io.circe.{Decoder, Encoder}

sealed trait ChildOrderType extends EnumEntry

object ChildOrderType extends Enum[ChildOrderType] {
  override def values = findValues

  case object LIMIT extends ChildOrderType

  case object MARKET extends ChildOrderType

  implicit val ChildOrderTypeEncoder: Encoder[ChildOrderType] =
    Encoder[String].contramap(_.entryName)

  implicit val ChildOrderTypeDecoder: Decoder[ChildOrderType] =
    Decoder[String].map(ChildOrderType.withName)

}
