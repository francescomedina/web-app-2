package it.polito.wa2.api.core.order

data class Order (
    var status: String? = "CREATED",
    var orderId: Int = 0,
    var price: Double? = 0.0,
    var buyer: Int? = 0,
    var amount: Int? = 0,
    var serviceAddress: String? = ""
)
//class Order {
//
//    var status: String = "CREATED"
//    var orderId: Int = 0
//    var price: Double = 0.0
//    var buyer: Int = 0
//    var amount: Int = 0
//    var serviceAddress: String? = ""
//
//    constructor() {
//        status = "CREATED"
//        orderId = 0
//        price = 0.0
//        buyer = 0
//        amount = 0
//        serviceAddress = "test"
//    }
//
//    constructor(
//        status: String,
//        orderId: Int,
//        price: Double,
//        buyer: Int,
//        amount: Int,
//        serviceAddress: String?
//    ) {
//        this.status = status
//        this.orderId = orderId
//        this.price = price
//        this.buyer = buyer
//        this.amount = amount
//        this.serviceAddress = serviceAddress
//    }
//}