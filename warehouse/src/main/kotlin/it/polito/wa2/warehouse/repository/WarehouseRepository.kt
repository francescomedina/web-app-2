package it.polito.wa2.warehouse.repository

import it.polito.wa2.warehouse.domain.WarehouseEntity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface WarehouseRepository : ReactiveMongoRepository<WarehouseEntity, String> {

    //fun findByOrderId(orderId: Int): Mono<WarehouseEntity?>
}