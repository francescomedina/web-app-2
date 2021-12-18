//package it.polito.wa2.api.core.order
//
//import org.springframework.web.bind.annotation.*
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//
//
//interface OrderService {
//
//    fun persistOrder(body: Order?): Order?
//
//    @PostMapping(value = ["/orders"], consumes = ["application/json"])
//    fun createOrder(body: Order?): Mono<Order?>?
//
//    /**
//     * Sample usage: "curl $HOST:$PORT/product/1".
//     *
//     * @param productId Id of the product
//     * @return the product, if found, else null
//     */
//    @GetMapping(value = ["/order/{orderId}"], produces = ["application/json"])
//    fun getOrder(@PathVariable orderId: Int): Mono<Order?>?
//
//    /**
//     * Sample usage: "curl $HOST:$PORT/product/1".
//     *
//     * @return the orders, if found, else null
//     */
//    @GetMapping(value = ["/order"], produces = ["application/json"])
//    fun getOrders(): Flux<Order?>?
//
//    fun updateStatus(order: Order, status: String)
//
//    @DeleteMapping(value = ["/order/{orderId}"], produces = ["application/json"])
//    fun deleteOrder(@PathVariable orderId: Int): Mono<Void?>?
//
//    @PutMapping(value = ["/order/{orderId}"], consumes = ["application/json"])
//    fun putOrder(body: Order?): Mono<Order?>?
//}
