package com.github.j5ik2o.bacs.model

import java.time.ZonedDateTime

import com.github.j5ik2o.bacs.api.JsonImplicits
import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class Chat(nickname: String, message: String, date: ZonedDateTime)

object Chat extends JsonImplicits {

  implicit val ChatEncoder: Encoder[Chat] = Encoder.instance { v =>
    Json.obj(
      "nickname" -> v.nickname.asJson,
      "message" -> v.message.asJson,
      "date" -> v.date.asJson
    )
  }

  implicit val ChatDecoder: Decoder[Chat] = Decoder.instance { hcursor =>
    for {
      nickname <- hcursor.get[String]("nickname")
      message <- hcursor.get[String]("message")
      date <- hcursor.get[ZonedDateTime]("date")
    } yield Chat(nickname, message, date)
  }

}
