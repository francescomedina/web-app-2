package it.polito.wa2.api.composite.catalog

data class OrderAggregate(
    val userId: Int = 0,
    val orders: List<OrderSummary>? = null
)

