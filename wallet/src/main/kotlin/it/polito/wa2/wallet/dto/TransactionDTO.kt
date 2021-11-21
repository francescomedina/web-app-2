package it.polito.wa2.wallet.dto

import it.polito.wa2.wallet.domain.TransactionEntity
import org.bson.types.ObjectId
import org.springframework.cloud.function.context.config.getSuspendingFunctionReturnType
import org.springframework.data.mongodb.core.mapping.FieldType
import org.springframework.data.mongodb.core.mapping.MongoId
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class TransactionDTO(
    var id: ObjectId? = null,

    @field:DecimalMin(value = "0.01", message="Amount must be greater than 0")
    @field:NotNull(message = "Missing required body parameter: amount")
    val amount: BigDecimal?,

    var time: Instant = Instant.now(),

    var senderWalletId: ObjectId? = null,

    @field:NotNull(message = "Missing required body parameter: receiverWalletId")
    val receiverWalletId: ObjectId?,
) {

}

fun TransactionEntity.toTransactionDTO() : TransactionDTO{
   return TransactionDTO(id = id, amount = amount, time=time, senderWalletId=senderWalletId, receiverWalletId=receiverWalletId)
}
