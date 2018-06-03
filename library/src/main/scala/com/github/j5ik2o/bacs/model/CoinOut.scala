package com.github.j5ik2o.bacs.model

case class CoinOut(id: Long,
                   order_id: String,
                   currency_code: String,
                   amount: BigDecimal,
                   address: String,
                   tx_hash: String,
                   fee: BigDecimal,
                   additional_fee: BigDecimal,
                   status: String,
                   event_date: String)
