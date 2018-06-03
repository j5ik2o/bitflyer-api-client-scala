package com.github.j5ik2o.bacs.model

case class Ticker(product_code: String,
                  timestamp: String,
                  tick_id: Long,
                  best_bid: Double,
                  best_ask: Double,
                  best_bid_size: Double,
                  best_ask_size: Double,
                  total_bid_depth: Double,
                  total_ask_depth: Double,
                  ltp: Double,
                  volume: Double,
                  volume_by_product: Double)
