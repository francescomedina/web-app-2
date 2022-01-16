package it.polito.wa2.warehouse.services

import it.polito.wa2.warehouse.domain.Category
import it.polito.wa2.warehouse.dto.ProductDTO
import it.polito.wa2.warehouse.dto.RatingDTO
import it.polito.wa2.warehouse.dto.UpdateProductDTO
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface ProductService {
    fun getProductsByCategory(category: String?): Flux<ProductDTO>
    suspend fun getProductByID(productID: String): ProductDTO
    fun createProduct(productDTO: ProductDTO): Mono<ProductDTO>
    fun updateProduct(productID: String, productDTO: ProductDTO): Mono<ProductDTO>
    suspend fun updatePartiallyProduct(productID: String, productDTO: UpdateProductDTO): Mono<ProductDTO>
    suspend fun deleteProductById(productID: String): Mono<Void>
    suspend fun getPictureById(productID: String): String
    suspend fun updatePictureById(productID: String,  pictureURL: String?): Mono<ProductDTO>
    suspend fun createRating(productID: String, ratingDTO: RatingDTO): Mono<ProductDTO>
}