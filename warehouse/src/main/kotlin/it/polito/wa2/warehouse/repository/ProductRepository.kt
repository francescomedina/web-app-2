package it.polito.wa2.warehouse.repository

import it.polito.wa2.warehouse.domain.Category
import it.polito.wa2.warehouse.domain.ProductEntity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux

interface ProductRepository : ReactiveMongoRepository<ProductEntity, String> {

    fun findAllByCategory(category: Category): Flux<ProductEntity?>

}