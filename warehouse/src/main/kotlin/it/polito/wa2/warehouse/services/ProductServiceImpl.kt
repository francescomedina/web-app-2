package it.polito.wa2.warehouse.services

import it.polito.wa2.warehouse.domain.Category
import it.polito.wa2.warehouse.domain.convertStringToEnum
import it.polito.wa2.warehouse.dto.ProductDTO
import it.polito.wa2.warehouse.dto.UpdateProductDTO
import it.polito.wa2.warehouse.dto.toProductDTO
import it.polito.wa2.warehouse.repository.ProductRepository
import kotlinx.coroutines.reactor.flux
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class ProductServiceImpl(
    val productRepository: ProductRepository
) : ProductService {

    override fun getProductsByCategory(category: Category?): Flux<ProductDTO> {
        return Flux.empty()

        TODO("Not yet implemented")
        //return if(category!=""){
        // Ask all products to the repository because we don't have a specific category
        /*return productRepository.findAll().map {
            it.toProductDTO()
        }*/

        /*} else {
            // Ask product of that category to the repository
            productRepository.findAllByCategory()
        }*/

    }

    override fun getProductByID(productID: String): ProductDTO {
        TODO("Not yet implemented")
    }

    override fun createProduct(productDTO: ProductDTO): ProductDTO {
        val categoryEnum: Category? = convertStringToEnum(productDTO.category)
        TODO("Not yet implemented")
    }

    override fun updateProduct(productID: String, productDTO: ProductDTO): ProductDTO {
        TODO("Not yet implemented")
    }

    override fun updatePartiallyProduct(productID: String, productDTO: UpdateProductDTO): ProductDTO {
        TODO("Not yet implemented")
    }

    override fun deleteProductById(productID: String): ProductDTO {
        TODO("Not yet implemented")
    }

    override fun getPictureById(productID: String): String {
        TODO("Not yet implemented")
    }

    override fun updatePictureById(productID: String): String {
        TODO("Not yet implemented")
    }

    override fun getWarehouseIdByProductID(productID: String): List<String> {
        TODO("Not yet implemented")
    }

}