package it.polito.wa2.warehouse.dto

import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import org.bson.types.ObjectId

data class ProductAvailabilityDTO(
    val id: ObjectId? = null,
    val min_quantity: Int,
    val productId: ObjectId,
    val quantity: Int
)

fun ProductAvailabilityEntity.toProductAvailabilityDTO(): ProductAvailabilityDTO {
    return ProductAvailabilityDTO(
        id = id,
        min_quantity = min_quantity,
        productId = productId,
        quantity = quantity
    )
}
