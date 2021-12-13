package it.polito.wa2.warehouse.controllers

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.util.jwt.JwtValidateUtils
import it.polito.wa2.warehouse.domain.Category
import it.polito.wa2.warehouse.domain.convertStringToEnum
import it.polito.wa2.warehouse.dto.ProductDTO
import it.polito.wa2.warehouse.dto.UpdateProductDTO
import it.polito.wa2.warehouse.services.ProductServiceImpl
import org.springframework.data.mongodb.core.query.Update

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux

import javax.validation.Valid

@RestController
@RequestMapping("/products")
class ProductController(
    val productServiceImpl: ProductServiceImpl,
    val jwtUtils: JwtValidateUtils,
) {

    /**
     * GET /products?category=<category>
     * This is a public endpoint
     * Retrieves the list of all products.
     * If the user specify the category it will retrieve all products by a given category
     * @param category: (optional) category name
     * @return all products or all products of that category
     */
    @GetMapping()
    suspend fun getProductsByCategory(
        @RequestParam("category", required = false) category: String?
    ): ResponseEntity<Flux<ProductDTO>> {

        try {
            // Check if category name is valid
            val categoryEnum: Category? = convertStringToEnum(category)

            // Ask the products to the service
            val productsDTO: Flux<ProductDTO> = productServiceImpl.getProductByCategory(categoryEnum)

            // Return a 200 with inside the transactions
            return ResponseEntity.status(HttpStatus.OK).body(productsDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }


    /**
     * GET /products/{productID}
     * This is a public endpoint
     * Retrieves the product identified by productID
     * @param productID: id of the product to retrieve
     * @return the product with that productID
     */
    @GetMapping("/products/{productID}")
    suspend fun getProductById(
        @PathVariable productID: String
    ): ResponseEntity<ProductDTO> {

        try {

            // Ask the product to the service
            val productDTO: ProductDTO = productServiceImpl.getProductByID(productID)

            // Return a 200 with inside the transactions
            return ResponseEntity.status(HttpStatus.OK).body(productDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }

    }

    /**
     * POST /products
     * Add a new product
     * Only Admin can access to this route
     * @param productDTO: name and price (description, pictureURL and category are optional)
     * @return the product created
     */
    @PostMapping()
    suspend fun createProduct(
        @RequestBody @Valid productDTO: ProductDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<ProductDTO> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to create the product
            val createdProductDTO: ProductDTO = productServiceImpl.createProduct(productDTO)

            // Return a 200 with inside the transactions
            return ResponseEntity.status(HttpStatus.CREATED).body(createdProductDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }


    /**
     * PUT /products/{productID}
     * Update an existing product (full representation), or add a new one if not exist
     * Only Admin can access to this route
     * @param productID: id of the product to update
     * @param productDTO: name and price (description, pictureURL and category are optional)
     * @return the product updated/created
     */
    @PutMapping("/{productID}")
    suspend fun updateProduct(
        @PathVariable productID: String,
        @RequestBody @Valid productDTO: ProductDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<ProductDTO>{
        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to update (entirely) the product
            val updatedProductDTO: ProductDTO = productServiceImpl.updateProduct(productID, productDTO)

            // Return a 200 with inside the transactions
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedProductDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

    /**
     * PATCH /products/{productID}
     * Update an existing product (partial representation)
     * Only Admin can access to this route
     * @param productID: id of the product to update
     * @param productDTO: name and price (description, pictureURL and category are optional)
     * @return the product updated
     */
    @PatchMapping("/{productID}")
    suspend fun updatePartiallyProduct(
        @PathVariable productID: String,
        @RequestBody @Valid productDTO: UpdateProductDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<ProductDTO>{
        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to update (entirely) the product
            val updatedProductDTO: ProductDTO = productServiceImpl.updatePartiallyProduct(productID, productDTO)

            // Return a 200 with inside the transactions
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedProductDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

    /**
     * DELETE /products/{productID}
     * Delete a product
     * Only Admin can access to this route
     * @param productID: id of the product to eliminate
     * @return the product deleted
     */
    @DeleteMapping("/{productID}")
    fun deleteProductById(
        @PathVariable productID: String,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<ProductDTO>{
        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to update (entirely) the product
            val deletedProductDTO: ProductDTO = productServiceImpl.deleteProductById(productID)

            // Return a 200 with inside the transactions
            return ResponseEntity.status(HttpStatus.OK).body(deletedProductDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

    /**
     * GET /products/{productID}/picture
     * Retrieves the picture of the product identified by productID
     * This is a public endpoint
     */
    fun getPictureById(

    ): ResponseEntity<String>{
        TODO("TO IMPLEMENT")
    }

    /**
     * POST /products/{productID}/picture
     * Updates the picture of the product identified by productID
     * Only Admin can access to this route
     */
    fun updatePictureById(

    ): ResponseEntity<String>{
        TODO("TO IMPLEMENT")
    }

    /**
     * GET /products/{productID}/warehouse
     * Gets the list of the warehouse
     * This is a public endpoint
     */
    fun getWarehouseByProductID(

    ): ResponseEntity<String>{
        TODO("TO IMPLEMENT")
    }
}


