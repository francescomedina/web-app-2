//package it.polito.wa2.api.core.order
//
//import org.springframework.data.annotation.PersistenceConstructor
//import org.springframework.data.mongodb.core.mapping.Document
//
////data class Order (
////    var status: String? = "CREATED",
////    var orderId: Int = 0,
////    var price: Double? = 0.0,
////    var buyer: Int? = 0,
////    var amount: Int? = 0,
////    var serviceAddress: String? = ""
////)
//@Document(collection = "order")
//class Order(
//    var status: String? = "CREATED",
//    var orderId: Int = 0,
//    var price: Double? = 0.0,
//    var buyer: Int? = 0,
//    var amount: Int? = 0,
//    var serviceAddress: String? = ""
//) {
//    lateinit var id: String
//        private set
//
//    @PersistenceConstructor
//    constructor(id: String, beverage: String, customerName: String) : this(beverage, customerName) {
//        this.id = id
//    }
//}