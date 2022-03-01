package it.polito.wa2.order.repositories

import it.polito.wa2.order.domain.OrderEntity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface OrderRepository : ReactiveMongoRepository<OrderEntity, String> {

    fun findAllByBuyer(buyer: String): Flux<OrderEntity>
}
