package it.polito.wa2.wallet.dto

import it.polito.wa2.wallet.domain.TransactionEntity
import it.polito.wa2.wallet.domain.WalletEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

data class WalletDTO(
    val id: String = "",

    @field:Min(0, message = "Amount must be positive")
    var amount: BigDecimal = BigDecimal(0.0),

    @field:NotBlank(message = "CustomerUsername is required")
    val customerUsername: String = "",

    val purchases: List<TransactionDTO> = emptyList(),
    val recharges: List<TransactionDTO> = emptyList(),
)

fun WalletEntity.toWalletDTO(): WalletDTO {
    return WalletDTO(
        id = id.toString(),
        amount = amount,
        customerUsername = customerUsername,
        purchases = purchases.map { it.toTransactionDTO() },
        recharges = recharges.map { it.toTransactionDTO() })
}