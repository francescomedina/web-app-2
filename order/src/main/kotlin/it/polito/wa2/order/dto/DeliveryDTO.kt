package it.polito.wa2.order.dto

import it.polito.wa2.order.domain.DeliveryEntity
import org.bson.types.ObjectId

data class DeliveryDTO(
    var shippingAddress: String?,
    var productId: ObjectId?,
    var warehouseId: ObjectId?
)

fun DeliveryEntity.toOrderDTO(): DeliveryDTO {
    return DeliveryDTO(
        shippingAddress = shippingAddress,
        productId = productId,
        warehouseId = warehouseId
    )
}