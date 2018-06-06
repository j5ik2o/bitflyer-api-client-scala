package com.github.j5ik2o.bacs

package object model {

  type PermissionsResponse = List[String]
  type ExecutionsResponse = List[Execution]
  type MarketsResponse = List[Market]
  type ChatsResponse = List[Chat]
  type BalancesResponse = List[Balance]
  type CollateralAccountsResponse = List[CollateralAccount]
  type AddressesResponse = List[Address]
  type CoinInsResponse = List[CoinIn]
  type CoinOutsResponse = List[CoinOut]
  type BankAccountsResponse = List[BankAccount]
  type DepositsResponse = List[Deposit]
  type WithdrawalsResponse = List[Withdrawal]
}
