package it.polito.wa2.order.dto

import it.polito.wa2.order.domain.DeliveryEntity
import org.bson.types.ObjectId

data class DeliveryDTO(
    var shippingAddress: String?,
    var warehouseId: ObjectId,
    var products: List<ProductDTO>
)

fun DeliveryEntity.toOrderDTO(): DeliveryDTO {
    return DeliveryDTO(
        shippingAddress = shippingAddress,
        warehouseId = warehouseId,
        products = products.map { it.toOrderDTO() }.toList()
    )
}