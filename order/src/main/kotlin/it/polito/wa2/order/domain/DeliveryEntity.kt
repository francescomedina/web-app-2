package it.polito.wa2.order.domain

import org.bson.types.ObjectId


data class DeliveryEntity (
    var shippingAddress: String?,
    var productId: ObjectId?,
    var warehouseId: ObjectId?
)
