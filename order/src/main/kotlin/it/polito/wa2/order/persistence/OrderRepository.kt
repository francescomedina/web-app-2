package it.polito.wa2.order.persistence

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface OrderRepository : ReactiveMongoRepository<OrderEntity, String> {

    fun findByOrderId(orderId: Int): Mono<OrderEntity?>
}