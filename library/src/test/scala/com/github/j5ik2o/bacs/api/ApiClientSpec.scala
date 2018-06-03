package com.github.j5ik2o.bacs.api

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{BeforeAndAfterAll, FreeSpecLike}

import scala.concurrent.duration._

class ApiClientSpec
    extends TestKit(ActorSystem("ApiClientSpec"))
    with FreeSpecLike
    with BeforeAndAfterAll
    with ScalaFutures {

  import system.dispatcher

  override implicit def patienceConfig: PatienceConfig =
    PatienceConfig(timeout = scaled(Span(10, Seconds)),
                   interval = scaled(Span(1, Seconds)))

  override def beforeAll(): Unit = {
    super.beforeAll()

  }

  override def afterAll(): Unit = {
    super.afterAll()
    TestKit.shutdownActorSystem(system)
  }

  val apiClient = new ApiClient(ApiConfig("api.bitflyer.jp", 443, 3 seconds))

  "ApiClient" - {

    "getMarkets" in {
      val result = apiClient.getMarkets().futureValue
      println(result)
    }
    "getBoard" in {
      val result = apiClient.getBoard().futureValue
      println(result)
    }
    "getTicker" in {
      val result = apiClient.getTicker().futureValue
      println(result)
    }
    "getBoardState" in {
      val result = apiClient.getBoardState().futureValue
      println(result)
    }
    "getHealth" in {
      val result = apiClient.getHealth().futureValue
      println(result)
    }
  }

}
