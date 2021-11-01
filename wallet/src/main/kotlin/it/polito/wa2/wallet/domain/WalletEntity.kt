package it.polito.wa2.wallet.domain

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal


@Document(collection = "wallet")
data class WalletEntity (
    @Id
    val id: ObjectId? = ObjectId.get(),

    var customerUsername: String = "",

    //TODO: Validate never less than 0
    var amount: BigDecimal = BigDecimal(0),

    var purchases : List<TransactionEntity> = mutableListOf(),
    var recharges : List<TransactionEntity> = mutableListOf(),

)