package it.polito.wa2.order.dto

import it.polito.wa2.order.domain.OrderEntity
import it.polito.wa2.order.domain.ProductEntity
import org.bson.types.ObjectId
import javax.validation.constraints.NotBlank

data class OrderDTO(
    var id: ObjectId? = null,
    @field:NotBlank(message = "Status is required")
    var status: String? = null,
    @field:NotBlank(message = "Buyer is required")
    var buyer: String? = null,

    var products: List<ProductDTO>? = emptyList(),
)

fun OrderEntity.toOrderDTO(): OrderDTO {
    return OrderDTO(
        id = id,
        status = status,
        buyer = buyer,
        products = products?.map { it.toOrderDTO() }?.toList()
    )
}
