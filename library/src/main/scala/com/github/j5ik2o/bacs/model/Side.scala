package com.github.j5ik2o.bacs.model

import enumeratum._
import io.circe.{Decoder, Encoder}

import scala.collection.immutable

sealed trait Side extends EnumEntry

object Side extends Enum[Side] {
  override def values: immutable.IndexedSeq[Side] = findValues

  case object BUY extends Side
  case object SELL extends Side

  implicit val SideEncoder: Encoder[Side] =
    Encoder[String].contramap(_.entryName)

  implicit val SideDecoder: Decoder[Side] = Decoder[String].map(Side.withName)

}
