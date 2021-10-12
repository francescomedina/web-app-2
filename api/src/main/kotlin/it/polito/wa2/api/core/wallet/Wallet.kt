package it.polito.wa2.api.core.wallet

data class Wallet (
    var status: String? = "CREATED",
    var orderId: Int = 0,
    var price: Double? = 0.0,
    var buyer: Int? = 0,
    var amount: Int? = 0,
    var serviceAddress: String? = ""
)
