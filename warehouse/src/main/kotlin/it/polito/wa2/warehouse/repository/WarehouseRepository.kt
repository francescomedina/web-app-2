package it.polito.wa2.warehouse.repository

import it.polito.wa2.warehouse.domain.WarehouseEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface WarehouseRepository : ReactiveMongoRepository<WarehouseEntity, String> {

    fun findOneByProductsProductIdAndProductsQuantityGreaterThanEqual (productId: ObjectId, quantity: Int) : Mono<WarehouseEntity?>

    fun findOneByIdAndProductsProductId (warehouseId: ObjectId, productId: ObjectId) : Mono<WarehouseEntity?>

    fun findAllByProductsProductId(productId: String): Flux<WarehouseEntity>
}