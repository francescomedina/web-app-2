package it.polito.wa2.api.core.order

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import reactor.core.publisher.Mono


interface OrderService {

    fun persistOrder(body: Order?): Order?

    @PostMapping(value = ["/orders"], consumes = ["application/json"])
    fun createOrder(body: Order?): Mono<Order?>?

    /**
     * Sample usage: "curl $HOST:$PORT/product/1".
     *
     * @param productId Id of the product
     * @return the product, if found, else null
     */
    @GetMapping(value = ["/order/{orderId}"], produces = ["application/json"])
    fun getOrder(@PathVariable orderId: Int): Mono<Order?>?

    fun updateStatus(order: Order, status: String)

    fun deleteOrder(orderId: Int): Mono<Void?>?
}