package com.github.j5ik2o.bacs.api

import java.time.ZonedDateTime

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.j5ik2o.bacs.model._
import io.circe.{Decoder, Json}
import io.circe.parser._
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

class ApiClient(config: ApiConfig)(implicit system: ActorSystem) {

  import io.circe.generic.auto._

  private val hmacUtils =
    new HmacUtils(HmacAlgorithms.HMAC_SHA_256, config.secretKey)

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
    val params = productCodeOpt.fold(Map.empty[String, String]) { v =>
      Map("product_code" -> v)
    } ++ countOpt.fold(Map.empty[String, String]) { v =>
      Map("count" -> v.toString)
    } ++ beforeOpt.fold(Map.empty[String, String]) { v =>
      Map("before" -> v.toString)
    } ++ afterOpt.fold(Map.empty[String, String]) { v =>
      Map("after" -> v.toString)
    }
    val responseFuture = Source
      .single(HttpRequest(
        uri = Uri("/v1/executions").withQuery(Uri.Query(params))) -> 1)
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

  private def createSignValue(ts: Long,
                              method: String,
                              path: String,
                              body: Option[String] = None) = {
    val text = s"$ts$method$path${body.getOrElse("")}"
    hmacUtils.hmacHex(text)
  }

  // --- Private API

  private def privateAccessHeaders(
      method: HttpMethod,
      path: String,
      body: Option[String] = None): Seq[RawHeader] = {
    val ts = ZonedDateTime.now.toInstant.toEpochMilli
    val sign = createSignValue(ts, method.value, path, body)
    Seq(RawHeader("ACCESS-KEY", config.accessKey),
        RawHeader("ACCESS-TIMESTAMP", ts.toString),
        RawHeader("ACCESS-SIGN", sign))
  }

  def getBalances()(implicit ec: ExecutionContext): Future[List[Balance]] = {
    val url = "/v1/me/getbalance"
    val responseFuture = Source
      .single(
        HttpRequest(uri = url).withHeaders(
          privateAccessHeaders(HttpMethods.GET, url): _*) -> 1)
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
      .single(
        HttpRequest(uri = url).withHeaders(
          privateAccessHeaders(HttpMethods.GET, url): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Collateral](Future.fromTry(triedResponse))
    }
  }

  def getCollateralAccounts()(
      implicit ec: ExecutionContext): Future[List[CollateralAccount]] = {
    val path = "/v1/me/getcollateralaccounts"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path).withHeaders(
          privateAccessHeaders(HttpMethods.GET, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[CollateralAccount]](Future.fromTry(triedResponse))
    }
  }

  def getAddresses()(implicit ec: ExecutionContext): Future[List[Address]] = {
    val url = "/v1/me/getaddresses"
    val responseFuture = Source
      .single(
        HttpRequest(uri = url).withHeaders(
          privateAccessHeaders(HttpMethods.GET, url): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[Address]](Future.fromTry(triedResponse))
    }
  }

  def getCoinIns(countOpt: Option[Int] = None,
                 beforeOpt: Option[Long] = None,
                 afterOpt: Option[Long] = None)(
      implicit ec: ExecutionContext): Future[List[CoinIn]] = {
    val params = countOpt.fold(Map.empty[String, String]) { v =>
      Map("count" -> v.toString)
    } ++ beforeOpt.fold(Map.empty[String, String]) { v =>
      Map("before" -> v.toString)
    } ++ afterOpt.fold(Map.empty[String, String]) { v =>
      Map("after" -> v.toString)
    }
    val uri = Uri("/v1/me/getcoinins").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(
        HttpRequest(uri = uri).withHeaders(
          privateAccessHeaders(HttpMethods.GET, uri.toString()): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[CoinIn]](Future.fromTry(triedResponse))
    }
  }

}
