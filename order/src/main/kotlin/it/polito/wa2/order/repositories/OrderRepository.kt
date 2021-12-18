package it.polito.wa2.order.repositories

import it.polito.wa2.order.domain.OrderEntity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface OrderRepository : ReactiveMongoRepository<OrderEntity, String>
