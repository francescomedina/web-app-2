package it.polito.wa2.wallet.dto

import it.polito.wa2.wallet.domain.TransactionEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.Instant
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class TransactionDTO(
    var id: ObjectId? = null,

    @field:NotNull(message = "Missing required body parameter: amount")
    val amount: BigDecimal?,

    var time: Instant = Instant.now(),

    var senderWalletId: ObjectId? = null,

    var receiverWalletId: ObjectId? = null,

    @field:NotBlank(message = "Reason field is required")
    val reason : String = ""
) {

}

fun TransactionEntity.toTransactionDTO() : TransactionDTO{
   return TransactionDTO(id = id, amount = amount, time=time, senderWalletId=senderWalletId, receiverWalletId=receiverWalletId, reason=reason)
}
