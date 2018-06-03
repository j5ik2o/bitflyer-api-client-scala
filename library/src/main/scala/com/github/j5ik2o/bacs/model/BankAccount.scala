package com.github.j5ik2o.bacs.model

case class BankAccount(id: Long,
                       is_verified: Boolean,
                       bank_name: String,
                       branch_name: String,
                       account_type: String,
                       account_number: String,
                       account_name: String)
