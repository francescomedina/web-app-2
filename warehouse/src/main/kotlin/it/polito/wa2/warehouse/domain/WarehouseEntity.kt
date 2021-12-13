package it.polito.wa2.warehouse.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "warehouse")
class WarehouseEntity {
    @Id
    var id: String? = null

    @Indexed(unique = true)
    var orderId = 0
    var status: String? = null
    var amount = 0
    var buyer = 0
    var price = 0.0

    override fun toString(): String {
        return String.format("ProductEntity: %s", orderId)
    }
}