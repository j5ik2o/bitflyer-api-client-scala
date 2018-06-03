package com.github.j5ik2o.bacs.api

import scala.concurrent.duration.FiniteDuration

case class ApiConfig(host: String,
                     port: Int = 443,
                     timeoutForToStrict: FiniteDuration,
                     accessKey: String,
                     secretKey: String)
