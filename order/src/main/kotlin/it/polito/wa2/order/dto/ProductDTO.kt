package it.polito.wa2.order.dto

import it.polito.wa2.order.domain.ProductEntity
import org.bson.types.ObjectId
import java.math.BigDecimal
import javax.validation.constraints.NotBlank

data class ProductDTO(
    var id: ObjectId? = null,

    @field:NotBlank(message = "Amount is required")
    var amount: BigDecimal? = null,
    @field:NotBlank(message = "Price is required")
    var price: BigDecimal? = null
)

fun ProductEntity.toOrderDTO(): ProductDTO {
    return ProductDTO(
        id = id,
        amount = amount,
        price = price
    )
}
