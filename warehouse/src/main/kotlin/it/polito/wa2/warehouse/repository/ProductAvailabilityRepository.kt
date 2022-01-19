package it.polito.wa2.warehouse.repository

import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ProductAvailabilityRepository: ReactiveMongoRepository<ProductAvailabilityEntity, String> {

    fun findOneByProductIdAndQuantityGreaterThanEqual (productId: ObjectId, quantity: Int) : Mono<ProductAvailabilityEntity?>

    fun findOneByWarehouseIdAndProductId (warehouseId: ObjectId, productId: ObjectId) : Mono<ProductAvailabilityEntity?>

    fun findAllByProductId(productId: ObjectId): Flux<ProductAvailabilityEntity>
}