package it.polito.wa2.order.domain

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "order")
data class OrderEntity (
    @Id
    var id: ObjectId? = ObjectId.get(),
    var status: String? = null,
    var buyer: String? = null,
    var products: List<ProductEntity>? = null,
    var delivery: List<DeliveryEntity>? = null
)
