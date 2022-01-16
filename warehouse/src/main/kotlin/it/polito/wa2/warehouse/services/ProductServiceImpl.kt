package it.polito.wa2.warehouse.services

import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.warehouse.domain.Category
import it.polito.wa2.warehouse.domain.ProductEntity
import it.polito.wa2.warehouse.domain.convertStringToEnum
import it.polito.wa2.warehouse.dto.ProductDTO
import it.polito.wa2.warehouse.dto.UpdateProductDTO
import it.polito.wa2.warehouse.dto.toProductDTO
import it.polito.wa2.warehouse.repository.ProductRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.flux
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class ProductServiceImpl(
    val productRepository: ProductRepository
) : ProductService {

    override fun getProductsByCategory(category: String?): Flux<ProductDTO> {

        if (category == null || category == "") {
            // Ask all products to the repository because we don't have a specific category

            return productRepository.findAll().map {
                it.toProductDTO()
            }
        } else {

            // Check if category name is valid
            val categoryEnum: Category = convertStringToEnum(category) ?:
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "Category can't be null")

            // Ask product of that category to the repository
            return productRepository.findAllByCategory(categoryEnum).mapNotNull {
                it?.toProductDTO()
            }
        }
    }

    override suspend fun getProductByID(productID: String): ProductDTO {
        // Take the information about the product with that productID
        val product = productRepository.findById(productID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Product not found")

        return product.toProductDTO()
    }

    override fun createProduct(productDTO: ProductDTO): Mono<ProductDTO> {
        val categoryEnum: Category? = convertStringToEnum(productDTO.category)

        if (productDTO.name == "" || productDTO.price == null) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Fields missing: name or price")
        }

        val newProduct: ProductEntity = ProductEntity(
            name = productDTO.name, description = productDTO.description,
            pictureURL = productDTO.pictureURL, category = categoryEnum, price = productDTO.price
        )

        return productRepository.save(newProduct).map {
            it.toProductDTO()
        }

    }

    override fun updateProduct(productID: String, productDTO: ProductDTO): Mono<ProductDTO> {
        val categoryEnum: Category? = convertStringToEnum(productDTO.category)

        if (productDTO.name == "" || productDTO.price == null) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Fields missing: name or price")
        }

        val newProduct = ProductEntity(id =  ObjectId(productID),
            name = productDTO.name, description = productDTO.description,
            pictureURL = productDTO.pictureURL, category = categoryEnum, price = productDTO.price
        )

        return productRepository.save(newProduct).map {
            it.toProductDTO()
        }

    }

    override suspend fun updatePartiallyProduct(productID: String, productDTO: UpdateProductDTO): Mono<ProductDTO> {

        val categoryEnum: Category? = convertStringToEnum(productDTO.category)

        val product = productRepository.findById(productID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Product not found")

        val updateProduct = ProductEntity(id = ObjectId(productID),
            name = productDTO.name ?: product.name,
            description = productDTO.description ?: product.description,
            pictureURL = productDTO.pictureURL ?: product.pictureURL,
            category = categoryEnum ?: product.category,
            price = productDTO.price ?: product.price
            )

        return productRepository.save(updateProduct).map {
            it.toProductDTO()
        }
    }

    override suspend fun deleteProductById(productID: String): Mono<Void> {
        val product = productRepository.findById(productID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Product not found")

        return productRepository.delete(product)
    }

    override suspend fun getPictureById(productID: String): String {
        val product = productRepository.findById(productID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Product not found")

        return product.pictureURL ?: ""
    }

    override suspend fun updatePictureById(productID: String, pictureURL: String?): Mono<ProductDTO> {
        if(pictureURL==null){
            throw ErrorResponse(HttpStatus.NOT_FOUND, "PictureURL not found")
        }

        val product = productRepository.findById(productID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Product not found")

        product.pictureURL = pictureURL

        return productRepository.save(product).map {
            it.toProductDTO()
        }
    }

    override fun getWarehouseIdByProductID(productID: String): List<String> {
        TODO("Not yet implemented")
    }

}