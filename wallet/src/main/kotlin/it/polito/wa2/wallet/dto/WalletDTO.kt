package it.polito.wa2.wallet.dto

import it.polito.wa2.wallet.domain.WalletEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotBlank

data class WalletDTO(
    var id: ObjectId? = null,

    @field:DecimalMin(value = "0.0", message="Amount must be positive")
    var amount: BigDecimal = BigDecimal(0.0),

    // This is the only field required
    @field:NotBlank(message = "CustomerUsername is required")
    val customerUsername: String = "",

    val purchases: List<TransactionDTO> = emptyList(),
    val recharges: List<TransactionDTO> = emptyList(),
)

fun WalletEntity.toWalletDTO(): WalletDTO {
    return WalletDTO(
        id = id,
        amount = amount,
        customerUsername = customerUsername,
        purchases = purchases.map { it.toTransactionDTO() },
        recharges = recharges.map { it.toTransactionDTO() })
}