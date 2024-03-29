package it.polito.wa2.warehouse.domain

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import javax.validation.constraints.Min

@Document("product-availability")
data class ProductAvailabilityEntity (
    @Id
    var id: ObjectId = ObjectId.get(),

    var productId: ObjectId,
    var warehouseId: ObjectId,
    @Min(0)
    var quantity: Int,
    @Min(0)
    var min_quantity: Int
)