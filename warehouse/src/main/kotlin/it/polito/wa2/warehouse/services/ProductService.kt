package it.polito.wa2.warehouse.services

import it.polito.wa2.warehouse.domain.Category
import it.polito.wa2.warehouse.dto.ProductDTO
import it.polito.wa2.warehouse.dto.UpdateProductDTO
import reactor.core.publisher.Flux

interface ProductService {
    fun getProductByCategory(category: Category?): Flux<ProductDTO>
    fun getProductByID(productID: String): ProductDTO
    fun createProduct(productDTO: ProductDTO): ProductDTO
    fun updateProduct(productID: String, productDTO: ProductDTO): ProductDTO
    fun updatePartiallyProduct(productID: String, productDTO: UpdateProductDTO): ProductDTO
    fun deleteProductById(productID: String): ProductDTO
}