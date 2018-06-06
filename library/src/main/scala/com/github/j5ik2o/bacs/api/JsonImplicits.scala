package com.github.j5ik2o.bacs.api

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

import com.github.j5ik2o.bacs.model.CurrencyCode
import io.circe.{Decoder, Encoder}

trait JsonImplicits {

  implicit val ZonedDateTimeEncoder: Encoder[ZonedDateTime] =
    Encoder[String].contramap(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(_))

  implicit val ZonedDateTimeDecoder: Decoder[ZonedDateTime] =
    Decoder[String].map { v =>
      ZonedDateTime.parse(
        v,
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.systemDefault()))
    }

  implicit val CurrencyCodeEncoder: Encoder[CurrencyCode] =
    Encoder[String].contramap(_.entryName)

  implicit val CurrencyCodeDecoder: Decoder[CurrencyCode] =
    Decoder[String].map(CurrencyCode.withName)

}
