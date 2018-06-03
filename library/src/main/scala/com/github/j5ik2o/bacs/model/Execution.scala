package com.github.j5ik2o.bacs.model

case class Execution(id: Long,
                     side: String,
                     price: Long,
                     size: Double,
                     exec_date: String,
                     buy_child_order_acceptance_id: String,
                     sell_child_order_acceptance_id: String)
