package com.github.j5ik2o.bacs.model

case class WithdrawRequest(currency_code: String,
                           bank_account_id: Long,
                           amount: BigDecimal,
                           code: String)
