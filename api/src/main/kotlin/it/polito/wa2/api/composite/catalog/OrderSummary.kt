package it.polito.wa2.api.composite.catalog

data class OrderSummary(
    val orderId: Int = 0,
    val name: String? = null,
    val weight: Int = 0,
    val serviceAddress: String? = null
)