package com.github.j5ik2o.bacs.api

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

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

object ApiClient {
  final val HEADER_NAME_ACCESS_KEY = "ACCESS-KEY"
  final val HEADER_NAME_ACCESS_TIMESTAMP = "ACCESS-TIMESTAMP"
  final val HEADER_NAME_ACCESS_SIGN = "ACCESS-SIGN"
  final val QUERY_PARAM_NAME_PRODUCT_CODE = "product_code"
}

class ApiClient(config: ApiConfig)(implicit system: ActorSystem) {
  import ApiClient._
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

  private def buildPagingParams(countOpt: Option[Int],
                                beforeOpt: Option[Long],
                                afterOpt: Option[Long]): Map[String, String] = {
    countOpt.fold(Map.empty[String, String]) { v =>
      Map("count" -> v.toString)
    } ++ beforeOpt.fold(Map.empty[String, String]) { v =>
      Map("before" -> v.toString)
    } ++ afterOpt.fold(Map.empty[String, String]) { v =>
      Map("after" -> v.toString)
    }

  }

  private def createSignValue(ts: Long,
                              method: String,
                              path: String,
                              body: Option[String] = None): String = {
    val text = s"$ts$method$path${body.getOrElse("")}"
    hmacUtils.hmacHex(text)
  }

  private def privateAccessHeaders(
      method: HttpMethod,
      path: String,
      body: Option[String] = None): Seq[RawHeader] = {
    val ts = ZonedDateTime.now.toInstant.toEpochMilli
    val sign = createSignValue(ts, method.value, path, body)
    Seq(
      RawHeader(HEADER_NAME_ACCESS_KEY, config.accessKey),
      RawHeader(HEADER_NAME_ACCESS_TIMESTAMP, ts.toString),
      RawHeader(HEADER_NAME_ACCESS_SIGN, sign)
    )
  }

  // --- Public API

  /**
    * マーケット一覧の取得。
    *
    * @param ec
    * @return
    */
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

  /**
    * 板情報の取得。
    *
    * @param productCodeOpt プロダクトコードもしくはエイリアス
    * @param ec [[ExecutionContext]]
    * @return [[Future[Board]]]
    */
  def getBoard(productCodeOpt: Option[String] = None)(
      implicit ec: ExecutionContext): Future[Board] = {
    val params = productCodeOpt
      .map(v => Map(QUERY_PARAM_NAME_PRODUCT_CODE -> v))
      .getOrElse(Map.empty[String, String])
    val uri = Uri("/v1/board").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Board](Future.fromTry(triedResponse))
    }
  }

  /**
    * ティッカーの取得。
    *
    * @param productCodeOpt プロダクトコードもしくはエイリアス
    * @param ec [[ExecutionContext]]
    * @return
    */
  def getTicker(productCodeOpt: Option[String] = None)(
      implicit ec: ExecutionContext): Future[Ticker] = {
    val params = productCodeOpt
      .map(v => Map(QUERY_PARAM_NAME_PRODUCT_CODE -> v))
      .getOrElse(Map.empty[String, String])
    val uri = Uri("/v1/ticker").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Ticker](Future.fromTry(triedResponse))
    }
  }

  /**
    * 約定履歴の取得。
    *
    * @param productCodeOpt プロダクトコードもしくはエイリアス
    * @param countOpt
    * @param beforeOpt
    * @param afterOpt
    * @param ec
    * @return
    */
  def getExecutions(productCodeOpt: Option[String] = None,
                    countOpt: Option[Int] = None,
                    beforeOpt: Option[Long] = None,
                    afterOpt: Option[Long] = None)(
      implicit ec: ExecutionContext): Future[Ticker] = {
    val params = productCodeOpt.fold(Map.empty[String, String]) { v =>
      Map(QUERY_PARAM_NAME_PRODUCT_CODE -> v)
    } ++ buildPagingParams(countOpt, beforeOpt, afterOpt)
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

  /**
    * 板状態の取得。
    *
    * @param productCodeOpt プロダクトコードもしくはエイリアス
    * @param ec
    * @return
    */
  def getBoardState(productCodeOpt: Option[String] = None)(
      implicit ec: ExecutionContext): Future[BoardState] = {
    val params = productCodeOpt
      .map(v => Map(QUERY_PARAM_NAME_PRODUCT_CODE -> v))
      .getOrElse(Map.empty[String, String])
    val uri = Uri("/v1/getboardstate").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[BoardState](Future.fromTry(triedResponse))
    }
  }

  /**
    * 取引所状態の取得。
    *
    * @param ec
    * @return
    */
  def getHealth()(implicit ec: ExecutionContext): Future[Health] = {
    val path = "/v1/gethealth"
    val responseFuture = Source
      .single(HttpRequest(uri = path) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Health](Future.fromTry(triedResponse))
    }
  }

  /**
    * チャットでの発言一覧の取得
    *
    * @param fromDateOpt 取得する発言日時
    * @param ec
    * @return
    */
  def getChats(fromDateOpt: Option[ZonedDateTime] = Some(
                 ZonedDateTime.now().minusSeconds(5)))(
      implicit ec: ExecutionContext): Future[List[Chat]] = {
    val params = fromDateOpt
      .map(v =>
        Map("from_date" -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(v)))
      .getOrElse(Map.empty[String, String])
    val uri = Uri("/v1/getchats").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[Chat]](Future.fromTry(triedResponse))
    }

  }

  // --- Private API

  /**
    * 呼出可能なエンドポイント一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 呼出可能なエンドポイント一覧
    */
  def getPermissions()(implicit ec: ExecutionContext): Future[List[String]] = {
    val path = "/v1/me/getpermissions"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path).withHeaders(
          privateAccessHeaders(HttpMethods.GET, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[String]](Future.fromTry(triedResponse))
    }
  }

  /**
    * 資産残高一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 資産残高一覧
    */
  def getBalances()(implicit ec: ExecutionContext): Future[List[Balance]] = {
    val path = "/v1/me/getbalance"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path).withHeaders(
          privateAccessHeaders(HttpMethods.GET, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[Balance]](Future.fromTry(triedResponse))
    }
  }

  /**
    * 証拠金状態の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 証拠金状態
    */
  def getCollateral()(implicit ec: ExecutionContext): Future[Collateral] = {
    val path = "/v1/me/getcollateral"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path).withHeaders(
          privateAccessHeaders(HttpMethods.GET, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[Collateral](Future.fromTry(triedResponse))
    }
  }

  /**
    * 通貨別の証拠金一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 通貨別の証拠金一覧
    */
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

  /**
    * 預入用アドレス一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 預入用アドレス一覧
    */
  def getAddresses()(implicit ec: ExecutionContext): Future[List[Address]] = {
    val path = "/v1/me/getaddresses"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path).withHeaders(
          privateAccessHeaders(HttpMethods.GET, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[Address]](Future.fromTry(triedResponse))
    }
  }

  /**
    * 仮想通貨預入履歴の取得。
    *
    * @param countOpt 結果の件数
    * @param beforeOpt このIDより小さいIDを持つデータを取得する
    * @param afterOpt このIDより大きいIDを持つデータを取得する
    * @param ec [[ExecutionContext]]
    * @return 仮想通貨預入履歴
    */
  def getCoinIns(countOpt: Option[Int] = None,
                 beforeOpt: Option[Long] = None,
                 afterOpt: Option[Long] = None)(
      implicit ec: ExecutionContext): Future[List[CoinIn]] = {
    val params = buildPagingParams(countOpt, beforeOpt, afterOpt)
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

  /**
    * 仮想通貨送付履歴の取得。
    *
    * @param countOpt 結果の件数
    * @param beforeOpt このIDより小さいIDを持つデータを取得する
    * @param afterOpt このIDより大きいIDを持つデータを取得する
    * @param ec [[ExecutionContext]]
    * @return 仮想通貨送付履歴
    */
  def getCoinOuts(countOpt: Option[Int] = None,
                  beforeOpt: Option[Long] = None,
                  afterOpt: Option[Long] = None)(
      implicit ec: ExecutionContext): Future[List[CoinOut]] = {
    val params = buildPagingParams(countOpt, beforeOpt, afterOpt)
    val uri = Uri("/v1/me/getcoinouts").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(
        HttpRequest(uri = uri).withHeaders(
          privateAccessHeaders(HttpMethods.GET, uri.toString()): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[CoinOut]](Future.fromTry(triedResponse))
    }
  }

  /**
    * 銀行口座一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 銀行口座一覧
    */
  def getBankAccounts()(
      implicit ec: ExecutionContext): Future[List[BankAccount]] = {
    val path = "/v1/me/getbankaccounts"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path).withHeaders(
          privateAccessHeaders(HttpMethods.GET, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[BankAccount]](Future.fromTry(triedResponse))
    }
  }

  /**
    * 入金履歴の取得。
    *
    * @param countOpt 結果の件数
    * @param beforeOpt このIDより小さいIDを持つデータを取得する
    * @param afterOpt このIDより大きいIDを持つデータを取得する
    * @param ec [[ExecutionContext]]
    * @return 入金履歴
    */
  def getDeposits(countOpt: Option[Int] = None,
                  beforeOpt: Option[Long] = None,
                  afterOpt: Option[Long] = None)(
      implicit ec: ExecutionContext): Future[List[Deposit]] = {
    val params = buildPagingParams(countOpt, beforeOpt, afterOpt)
    val uri = Uri("/v1/me/getdeposits").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(
        HttpRequest(uri = uri).withHeaders(
          privateAccessHeaders(HttpMethods.GET, uri.toString()): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[List[Deposit]](Future.fromTry(triedResponse))
    }
  }

}
