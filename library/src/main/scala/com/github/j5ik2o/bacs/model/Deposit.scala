package com.github.j5ik2o.bacs.model

case class Deposit(id: Long,
                   order_id: String,
                   currency_code: String,
                   amount: BigDecimal,
                   status: String,
                   event_date: String)
