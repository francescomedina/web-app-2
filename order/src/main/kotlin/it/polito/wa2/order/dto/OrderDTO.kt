package it.polito.wa2.order.dto

import it.polito.wa2.order.domain.OrderEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotBlank

data class OrderDTO(
    var id: ObjectId? = null,

    @field:DecimalMin(value = "0.0", message="Amount must be positive")
    var amount: BigDecimal = BigDecimal(0.0),

    // This is the only field required
    @field:NotBlank(message = "CustomerUsername is required")
    val customerUsername: String = "",
)

fun OrderEntity.toOrderDTO(): OrderDTO {
    return OrderDTO(
        id = id,
        amount = amount,
        customerUsername = customerUsername
    )
}
