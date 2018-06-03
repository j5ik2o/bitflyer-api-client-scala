package com.github.j5ik2o.bacs.model

case class BoardStateData(special_quotation: Long)
case class BoardState(health: String,
                      state: String,
                      data: Option[BoardStateData])
