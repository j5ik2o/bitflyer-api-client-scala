package com.github.j5ik2o.bacs.model

case class Ticker(product_code: String,
                  timestamp: String,
                  tick_id: Long,
                  best_bid: BigDecimal,
                  best_ask: BigDecimal,
                  best_bid_size: BigDecimal,
                  best_ask_size: BigDecimal,
                  total_bid_depth: BigDecimal,
                  total_ask_depth: BigDecimal,
                  ltp: BigDecimal,
                  volume: BigDecimal,
                  volume_by_product: BigDecimal)
