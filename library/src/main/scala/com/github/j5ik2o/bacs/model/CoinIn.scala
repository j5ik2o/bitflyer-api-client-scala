package com.github.j5ik2o.bacs.model

case class CoinIn(id: Long,
                  order_id: String,
                  currency_code: String,
                  amount: BigDecimal,
                  address: String,
                  tx_hash: String,
                  status: String,
                  event_date: String)
