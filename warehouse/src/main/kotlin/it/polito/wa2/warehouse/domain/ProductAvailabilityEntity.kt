package it.polito.wa2.warehouse.domain

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document("product-availability")
data class ProductAvailabilityEntity (
    @Id
    var id: ObjectId = ObjectId.get(),

    var productId: ObjectId,
    var warehouseId: ObjectId,
    var quantity: Int,
    var min_quantity: Int
)