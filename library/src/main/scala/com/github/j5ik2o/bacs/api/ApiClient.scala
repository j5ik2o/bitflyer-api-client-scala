package com.github.j5ik2o.bacs.api

import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers.RawHeader
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.github.j5ik2o.bacs.model._
import io.circe.parser._
import io.circe.syntax._
import io.circe.{Decoder, Json}
import org.apache.commons.codec.digest.{HmacAlgorithms, HmacUtils}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

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
  def getMarkets()(implicit ec: ExecutionContext): Future[MarketsResponse] = {
    val method = HttpMethods.GET
    val url = "/v1/markets"
    val responseFuture = Source
      .single(HttpRequest(uri = url, method = method) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[MarketsResponse](Future.fromTry(triedResponse))
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
      implicit ec: ExecutionContext): Future[BoardResponse] = {
    val params = productCodeOpt
      .map(v => Map(QUERY_PARAM_NAME_PRODUCT_CODE -> v))
      .getOrElse(Map.empty[String, String])
    val method = HttpMethods.GET
    val uri = Uri("/v1/board").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri, method = method) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[BoardResponse](Future.fromTry(triedResponse))
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
      implicit ec: ExecutionContext): Future[TickerResponse] = {
    val params = productCodeOpt
      .map(v => Map(QUERY_PARAM_NAME_PRODUCT_CODE -> v))
      .getOrElse(Map.empty[String, String])
    val method = HttpMethods.GET
    val uri = Uri("/v1/ticker").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri, method = method) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[TickerResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 約定履歴の取得。
    *
    * @param productCodeOpt プロダクトコードもしくはエイリアス
    * @param countOpt
    * @param beforeOpt
    * @param afterOpt
    * @param ec [[ExecutionContext]]
    * @return
    */
  def getExecutions(productCodeOpt: Option[String] = None,
                    countOpt: Option[Int] = None,
                    beforeOpt: Option[Long] = None,
                    afterOpt: Option[Long] = None)(
      implicit ec: ExecutionContext): Future[ExecutionsResponse] = {
    val params = productCodeOpt.fold(Map.empty[String, String]) { v =>
      Map(QUERY_PARAM_NAME_PRODUCT_CODE -> v)
    } ++ buildPagingParams(countOpt, beforeOpt, afterOpt)
    val method = HttpMethods.GET
    val uri = Uri("/v1/executions").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri, method = method) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[ExecutionsResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 板状態の取得。
    *
    * @param productCodeOpt プロダクトコードもしくはエイリアス
    * @param ec [[ExecutionContext]]
    * @return
    */
  def getBoardState(productCodeOpt: Option[String] = None)(
      implicit ec: ExecutionContext): Future[BoardStateResponse] = {
    val params = productCodeOpt
      .map(v => Map(QUERY_PARAM_NAME_PRODUCT_CODE -> v))
      .getOrElse(Map.empty[String, String])
    val method = HttpMethods.GET
    val uri = Uri("/v1/getboardstate").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri, method = method) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[BoardStateResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 取引所状態の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return
    */
  def getHealth()(implicit ec: ExecutionContext): Future[HealthResponse] = {
    val method = HttpMethods.GET
    val path = "/v1/gethealth"
    val responseFuture = Source
      .single(HttpRequest(uri = path, method = method) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[HealthResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * チャットでの発言一覧の取得
    *
    * @param fromDateOpt 取得する発言日時
    * @param ec [[ExecutionContext]]
    * @return
    */
  def getChats(fromDateOpt: Option[ZonedDateTime] = Some(
                 ZonedDateTime.now().minusSeconds(5)))(
      implicit ec: ExecutionContext): Future[ChatsResponse] = {
    val params = fromDateOpt
      .map(v =>
        Map("from_date" -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(v)))
      .getOrElse(Map.empty[String, String])
    val method = HttpMethods.GET
    val uri = Uri("/v1/getchats").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(HttpRequest(uri = uri, method = method) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[ChatsResponse](Future.fromTry(triedResponse))
    }

  }

  // --- Private API

  /**
    * 呼出可能なエンドポイント一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 呼出可能なエンドポイント一覧
    */
  def getPermissions()(
      implicit ec: ExecutionContext): Future[PermissionsResponse] = {
    val method = HttpMethods.GET
    val path = "/v1/me/getpermissions"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path, method = method).withHeaders(
          privateAccessHeaders(method, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[PermissionsResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 資産残高一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 資産残高一覧
    */
  def getBalances()(implicit ec: ExecutionContext): Future[BalancesResponse] = {
    val method = HttpMethods.GET
    val path = "/v1/me/getbalance"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path, method = method).withHeaders(
          privateAccessHeaders(method, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[BalancesResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 証拠金状態の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 証拠金状態
    */
  def getCollateral()(
      implicit ec: ExecutionContext): Future[CollateralResponse] = {
    val method = HttpMethods.GET
    val path = "/v1/me/getcollateral"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path, method = method).withHeaders(
          privateAccessHeaders(method, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[CollateralResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 通貨別の証拠金一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 通貨別の証拠金一覧
    */
  def getCollateralAccounts()(
      implicit ec: ExecutionContext): Future[CollateralAccountsResponse] = {
    val method = HttpMethods.GET
    val path = "/v1/me/getcollateralaccounts"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path, method = method).withHeaders(
          privateAccessHeaders(method, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[CollateralAccountsResponse](
          Future.fromTry(triedResponse))
    }
  }

  /**
    * 預入用アドレス一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 預入用アドレス一覧
    */
  def getAddresses()(
      implicit ec: ExecutionContext): Future[AddressesResponse] = {
    val method = HttpMethods.GET
    val path = "/v1/me/getaddresses"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path, method = method).withHeaders(
          privateAccessHeaders(method, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[AddressesResponse](Future.fromTry(triedResponse))
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
      implicit ec: ExecutionContext): Future[CoinInsResponse] = {
    val method = HttpMethods.GET
    val params = buildPagingParams(countOpt, beforeOpt, afterOpt)
    val uri = Uri("/v1/me/getcoinins").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(
        HttpRequest(uri = uri, method = method).withHeaders(
          privateAccessHeaders(method, uri.toString()): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[CoinInsResponse](Future.fromTry(triedResponse))
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
      implicit ec: ExecutionContext): Future[CoinOutsResponse] = {
    val params = buildPagingParams(countOpt, beforeOpt, afterOpt)
    val method = HttpMethods.GET
    val uri = Uri("/v1/me/getcoinouts").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(
        HttpRequest(uri = uri, method = method).withHeaders(
          privateAccessHeaders(method, uri.toString()): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[CoinOutsResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 銀行口座一覧の取得。
    *
    * @param ec [[ExecutionContext]]
    * @return 銀行口座一覧
    */
  def getBankAccounts()(
      implicit ec: ExecutionContext): Future[BankAccountsResponse] = {
    val method = HttpMethods.GET
    val path = "/v1/me/getbankaccounts"
    val responseFuture = Source
      .single(
        HttpRequest(uri = path, method = method).withHeaders(
          privateAccessHeaders(method, path): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[BankAccountsResponse](Future.fromTry(triedResponse))
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
      implicit ec: ExecutionContext): Future[DepositsResponse] = {
    val params = buildPagingParams(countOpt, beforeOpt, afterOpt)
    val method = HttpMethods.GET
    val uri = Uri("/v1/me/getdeposits").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(
        HttpRequest(uri = uri, method = method).withHeaders(
          privateAccessHeaders(method, uri.toString()): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[DepositsResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 出金。
    *
    * @param ec [[ExecutionContext]]
    * @return 出金結果
    */
  def withdraw(withdraw: WithdrawRequest)(
      implicit ec: ExecutionContext): Future[WithdrawResponse] = {
    val method = HttpMethods.POST
    val path = "/v1/me/withdraw"
    val bytes = withdraw.asJson.noSpaces.getBytes(StandardCharsets.UTF_8)
    val responseFuture = Source
      .single(
        HttpRequest(uri = path, method = method)
          .withHeaders(privateAccessHeaders(method, path): _*)
          .withEntity(bytes) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[WithdrawResponse](Future.fromTry(triedResponse))
    }
  }

  /**
    * 出金履歴。
    *
    * @param messageIdOpt
    * @param countOpt
    * @param beforeOpt
    * @param afterOpt
    * @param ec [[ExecutionContext]]
    * @return
    */
  def getWtihdrawals(messageIdOpt: Option[Long] = None,
                     countOpt: Option[Int] = None,
                     beforeOpt: Option[Long] = None,
                     afterOpt: Option[Long] = None)(
      implicit ec: ExecutionContext): Future[WithdrawalsResponse] = {
    val params = messageIdOpt.fold(Map.empty[String, String]) { v =>
      Map("message_id" -> v.toString)
    } ++ buildPagingParams(countOpt, beforeOpt, afterOpt)
    val method = HttpMethods.GET
    val uri = Uri("/v1/me/getwithdrawals").withQuery(Uri.Query(params))
    val responseFuture = Source
      .single(
        HttpRequest(uri = uri, method = method).withHeaders(
          privateAccessHeaders(method, uri.toString()): _*) -> 1)
      .via(poolClientFlow)
      .runWith(Sink.head)
    responseFuture.flatMap {
      case (triedResponse, _) =>
        responseToModel[WithdrawalsResponse](Future.fromTry(triedResponse))
    }
  }

}
