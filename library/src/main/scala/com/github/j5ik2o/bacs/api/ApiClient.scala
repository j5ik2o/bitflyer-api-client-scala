package com.github.j5ik2o.bacs.api

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.j5ik2o.bacs.model._
import io.circe.{Decoder, Json}
import io.circe.parser._
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

case class JsonParsingException(message: String) extends Exception(message)

case class JsonDecodingException(message: String) extends Exception(message)

class ApiClient(config: ApiConfig)(implicit system: ActorSystem) {

  import io.circe.generic.auto._

  private implicit val materializer = ActorMaterializer()
  private val poolClientFlow =
    Http().cachedHostConnectionPoolHttps[Int](config.host, config.port)
  private val timeout: FiniteDuration = config.timeoutForToStrict

  private def toJson(jsonString: String): Future[Json] = {
    parse(jsonString) match {
      case Right(r) =>
        println(r.spaces2)
        Future.successful(r)
      case Left(error) => Future.failed(JsonParsingException(error.message))
    }
  }

  private def toModel[A](json: Json)(implicit d: Decoder[A]): Future[A] = {
    json.as[A] match {
      case Right(r)    => Future.successful(r)
      case Left(error) => Future.failed(JsonDecodingException(error.message))
    }
  }

  private def responseToModel[A](responseFuture: Future[HttpResponse])(
      implicit d: Decoder[A],
      ec: ExecutionContext): Future[A] = {
    for {
      httpResponse <- responseFuture
      httpEntity <- httpResponse.entity.toStrict(timeout)
      json <- toJson(httpEntity.data.utf8String)
      model <- toModel(json)
    } yield model
  }

  def getMarkets()(implicit ec: ExecutionContext): Future[List[Market]] = {
    val url = "/v1/markets"
    val responseFuture = Source
      .single(HttpRequest(uri = url) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[Market]](Future.fromTry(triedResponse))
    }
  }

  def getBoard(productCodeOpt: Option[String] = None)(
      implicit ec: ExecutionContext): Future[Board] = {
    val url =
      s"/v1/board${productCodeOpt.fold("")(v => s"product_code=$v")}"
    val responseFuture = Source
      .single(HttpRequest(uri = url) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Board](Future.fromTry(triedResponse))
    }
  }

  def getTicker(productCodeOpt: Option[String] = None)(
      implicit ec: ExecutionContext): Future[Ticker] = {
    val url =
      s"/v1/ticker${productCodeOpt.fold("")(v => s"product_code=$v")}"
    val responseFuture = Source
      .single(HttpRequest(uri = url) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Ticker](Future.fromTry(triedResponse))
    }
  }

  def getExecutions(productCodeOpt: Option[String] = None,
                    countOpt: Option[Int] = None,
                    beforeOpt: Option[Long] = None,
                    afterOpt: Option[Long] = None)(
      implicit ec: ExecutionContext): Future[Ticker] = {
    val url =
      s"/v1/executions${productCodeOpt.fold("")(v => s"product_code=$v")}${countOpt
        .fold("")(v => s"count=$v")}${beforeOpt.fold("")(v => s"before=$v")}${afterOpt
        .fold("")(v => s"after=$v")}"
    val responseFuture = Source
      .single(HttpRequest(uri = url) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Ticker](Future.fromTry(triedResponse))
    }
  }

  def getBoardState(productCodeOpt: Option[String] = None)(
      implicit ec: ExecutionContext): Future[BoardState] = {
    val url =
      s"/v1/getboardstate${productCodeOpt.fold("")(v => s"product_code=$v")}"
    val responseFuture = Source
      .single(HttpRequest(uri = url) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[BoardState](Future.fromTry(triedResponse))
    }
  }

  def getHealth()(implicit ec: ExecutionContext): Future[Health] = {
    val url = "/v1/gethealth"
    val responseFuture = Source
      .single(HttpRequest(uri = url) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Health](Future.fromTry(triedResponse))
    }
  }

  private def calcSignValue(ts: Long,
                            method: String,
                            path: String,
                            body: Option[String] = None) = {
    val text = s"$ts$method$path${body.getOrElse("")}"
    new HmacUtils(HmacAlgorithms.HMAC_SHA_256, config.secretKey).hmacHex(text)
  }

  private def privateAccessHeaders(
      method: String,
      path: String,
      body: Option[String] = None): Seq[RawHeader] = {
    val ts = ZonedDateTime.now.toInstant.toEpochMilli
    val sign = calcSignValue(ts, method, path, body)
    Seq(RawHeader("ACCESS-KEY", config.accessKey),
        RawHeader("ACCESS-TIMESTAMP", ts.toString),
        RawHeader("ACCESS-SIGN", sign))
  }

  def getBalances()(implicit ec: ExecutionContext): Future[List[Balance]] = {
    val url = "/v1/me/getbalance"
    val responseFuture = Source
      .single(HttpRequest(uri = url).withHeaders(
        privateAccessHeaders("GET", url): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[Balance]](Future.fromTry(triedResponse))
    }
  }

  def getCollateral()(implicit ec: ExecutionContext): Future[Collateral] = {
    val url = "/v1/me/getcollateral"
    val responseFuture = Source
      .single(HttpRequest(uri = url).withHeaders(
        privateAccessHeaders("GET", url): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Collateral](Future.fromTry(triedResponse))
    }

  }
}
