package com.github.j5ik2o.bacs.model

case class Bid(price: Long, size: Double)
case class Ask(price: Long, size: Double)

case class Board(mid_price: Long, bids: List[Bid], asks: List[Ask])
