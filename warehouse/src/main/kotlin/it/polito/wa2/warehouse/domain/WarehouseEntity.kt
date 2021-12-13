package it.polito.wa2.warehouse.domain

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "warehouse")
data class WarehouseEntity (
    @Id
    val id: ObjectId = ObjectId.get(),

    val productId: ObjectId,
    val warehouseId: ObjectId,
    val quantity: Int,
    val min_quantity: Int

)