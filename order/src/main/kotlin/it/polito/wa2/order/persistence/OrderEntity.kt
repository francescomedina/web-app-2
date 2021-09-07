package it.polito.wa2.order.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "order")
class OrderEntity {
    @Id
    var id: String? = null

    @Indexed(unique = true)
    var orderId = 0
    var status: String? = null
    var amount = 0
    var buyer = 0
    var price = 0.0

    constructor() {}
    constructor(orderId: Int, status: String?, amount: Int, buyer: Int, price: Double) {
        this.orderId = orderId
        this.status = status
        this.amount = amount
        this.buyer = buyer
        this.price = price
    }

    override fun toString(): String {
        return String.format("ProductEntity: %s", orderId)
    }
}