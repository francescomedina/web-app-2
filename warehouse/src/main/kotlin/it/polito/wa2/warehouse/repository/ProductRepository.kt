package it.polito.wa2.warehouse.repository

import it.polito.wa2.warehouse.domain.Category
import it.polito.wa2.warehouse.domain.ProductEntity

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ProductRepository : ReactiveMongoRepository<ProductEntity, String> {

    fun findAllByCategory(category: Enum<Category>): Flux<ProductEntity?>

}