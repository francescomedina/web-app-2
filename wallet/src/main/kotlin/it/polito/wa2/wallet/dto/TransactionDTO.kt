package it.polito.wa2.wallet.dto

import it.polito.wa2.wallet.domain.TransactionEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class TransactionDTO(
    var id: ObjectId? = null,

    @NotNull
    @field:Min(0, message="Amount must be positive")
    var amount: BigDecimal = BigDecimal(0.0),
    @NotNull
    var time: Instant = Instant.now(),
    @NotNull
    val senderWalletId: ObjectId,
    @NotNull
    val receiverWalletId: ObjectId
) {
}

//fun Transaction.toTransactionDTO() = TransactionDTO(id, amount, time, sender.id!!, receiver.id!!)

fun TransactionEntity.toTransactionDTO() : TransactionDTO{
   return TransactionDTO(id = id, amount = amount, time=time, senderWalletId=senderWalletId, receiverWalletId=receiverWalletId)
}
