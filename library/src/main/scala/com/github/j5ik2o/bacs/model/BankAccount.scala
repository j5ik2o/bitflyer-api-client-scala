package com.github.j5ik2o.bacs.model

import io.circe.{Decoder, Encoder, Json}
import io.circe.syntax._

case class BankAccount(id: Long,
                       isVerified: Boolean,
                       bankName: String,
                       branchName: String,
                       accountType: String,
                       accountNumber: String,
                       accountName: String)

object BankAccount {

  implicit val BankAccountEncoder: Encoder[BankAccount] = Encoder.instance {
    v =>
      Json.obj(
        "id" -> v.id.asJson,
        "is_verified" -> v.isVerified.asJson,
        "bank_name" -> v.bankName.asJson,
        "branch_name" -> v.branchName.asJson,
        "account_type" -> v.accountType.asJson,
        "account_number" -> v.accountNumber.asJson,
        "account_name" -> v.accountName.asJson
      )
  }

  implicit val BankAccountDecoder: Decoder[BankAccount] = Decoder.instance {
    hcursor =>
      for {
        id <- hcursor.get[Long]("id")
        isVerified <- hcursor.get[Boolean]("is_verified")
        bankName <- hcursor.get[String]("bank_name")
        branchName <- hcursor.get[String]("branch_name")
        accountType <- hcursor.get[String]("account_type")
        accountNumber <- hcursor.get[String]("account_number")
        accountName <- hcursor.get[String]("account_name")
      } yield
        BankAccount(id,
                    isVerified,
                    bankName,
                    branchName,
                    accountType,
                    accountNumber,
                    accountName)
  }

}
