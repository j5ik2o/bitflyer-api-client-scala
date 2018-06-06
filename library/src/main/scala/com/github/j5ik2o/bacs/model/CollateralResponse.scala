package com.github.j5ik2o.bacs.model

case class CollateralResponse(collateral: BigDecimal,
                              open_position_pnl: BigDecimal,
                              require_collateral: BigDecimal,
                              keep_rate: BigDecimal)
