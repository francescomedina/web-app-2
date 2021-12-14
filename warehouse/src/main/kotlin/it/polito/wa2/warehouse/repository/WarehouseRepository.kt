package it.polito.wa2.warehouse.repository

import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface WarehouseRepository : ReactiveMongoRepository<ProductAvailabilityEntity, String> {

    //fun findByOrderId(orderId: Int): Mono<WarehouseEntity?>
}