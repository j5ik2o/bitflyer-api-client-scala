package com.github.j5ik2o.bacs.model

case class CoinOut(id: Long,
                   order_id: String,
                   currency_code: String,
                   amount: Double,
                   address: String,
                   tx_hash: String,
                   fee: Double,
                   additional_fee: Double,
                   status: String,
                   event_date: String)
