package it.polito.wa2.warehouse.controllers

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.util.jwt.JwtValidateUtils
import it.polito.wa2.warehouse.dto.ProductAvailabilityDTO
import it.polito.wa2.warehouse.dto.WarehouseDTO
import it.polito.wa2.warehouse.services.PartiallyProductAvailabilityDTO
import it.polito.wa2.warehouse.services.PartiallyWarehouseDTO
import it.polito.wa2.warehouse.services.WarehouseServiceImpl
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import javax.validation.Valid

@RestController
@RequestMapping("/warehouses")
class WarehouseController(
    val warehouseServiceImpl: WarehouseServiceImpl,
    val jwtUtils: JwtValidateUtils,
) {
    /**
     * GET /warehouses
     * Retrieves the list of all warehouses.
     * @return the list of all warehouses
     */
    @GetMapping("/")
    suspend fun getWarehouses(): ResponseEntity<MutableList<WarehouseDTO>> {
        try {
            // Ask the list of warehouses to the service
            val warehouses = warehouseServiceImpl.getWarehouses().collectList().awaitSingle()

            // Return a 200 with inside list of all warehouses
            return ResponseEntity.status(HttpStatus.CREATED).body(warehouses)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }


    /**
     * GET /warehouses/{warehouseID}
     * Retrieves the warehouse identified by warehouseID
     * @return the list of all warehouses
     */
    @GetMapping("/{warehouseID}")
    suspend fun getWarehouseByID(
        @PathVariable warehouseID: String,
    ): ResponseEntity<WarehouseDTO> {
        try {

            // Ask the warehouse information with that warehouseID to the service
            val warehouseDTO: WarehouseDTO = warehouseServiceImpl.getWarehouseByID(warehouseID)

            // Return a 200 with inside the warehouse information
            return ResponseEntity.status(HttpStatus.OK).body(warehouseDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }


    /**
     * POST /warehouses
     * Add a new warehouse
     * @param warehouseDTO
     * @return the warehouse created
     */
    @PostMapping("/")
    suspend fun createWarehouse(
        @RequestBody @Valid warehouseDTO: WarehouseDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ) : ResponseEntity<Mono<WarehouseDTO>> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to create a new warehouse
            val createdWarehouse = warehouseServiceImpl.createWarehouse(warehouseDTO)

            // Return a 200 with inside the warehouse information
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWarehouse)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }


    /**
     * PUT /warehouses/{warehouseID}
     * Updates an existing warehouse (full representation), or adds a new one if not exist
     * @param warehouseID: id of the warehouse to update
     * @param warehouseDTO:
     * @return the warehouse updated/created
     */
    @PutMapping("/{warehouseID}")
    suspend fun updateWarehouse(
        @PathVariable warehouseID: String,
        @RequestBody @Valid warehouseDTO: WarehouseDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ) : ResponseEntity<Mono<WarehouseDTO>> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to create a new warehouse
            val updatedWarehouse = warehouseServiceImpl.updateWarehouse(warehouseID,warehouseDTO)

            // Return a 200 with inside the warehouse information
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedWarehouse)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

    /**
     * PATCH /warehouses/{warehouseID}
     * Updates an existing warehouse (partial representation)
     * @param warehouseID: id of the warehouse to update
     * @param warehouseDTO:
     * @return the warehouse updated
     */
    @PatchMapping("/{warehouseID}")
    suspend fun updatePartiallyWarehouse(
        @PathVariable warehouseID: String,
        @RequestBody @Valid warehouseDTO: PartiallyWarehouseDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ) : ResponseEntity<Mono<WarehouseDTO>> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to create a new warehouse
            val updatedWarehouse = warehouseServiceImpl.updatePartiallyWarehouse(warehouseID,warehouseDTO)

            // Return a 200 with inside the warehouse information
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedWarehouse)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

    /**
     * DELETE /warehouses/{warehouseID}
     * Deletes a warehouse
     * @param warehouseID: id of the warehouse to update
     * @return the warehouse deleted
     */
    @DeleteMapping("/{warehouseID}")
    suspend fun deleteWarehouseById(
        @PathVariable warehouseID: String,
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<Mono<Void>> {
        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            val deletedWarehouseDTO: Mono<Void> = warehouseServiceImpl.deleteWarehouse(warehouseID)

            return ResponseEntity.status(HttpStatus.OK).body(deletedWarehouseDTO)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

    /**
     * POST /warehouses/{warehouseID}/products
     * Add a product with a given initial quantity to the warehouse
     * @param warehouseID:
     * @param productAvailabilityDTO:
     * @return the new product with quantity
     */
    @PostMapping("/{warehouseID}/products")
    suspend fun createProductAvailability(
        @PathVariable warehouseID: String,
        @RequestHeader(name = "Authorization") jwtToken: String,
        @RequestBody @Valid productAvailabilityDTO: ProductAvailabilityDTO,
    ) : ResponseEntity<Mono<ProductAvailabilityDTO>> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            val updatedWarehouse = warehouseServiceImpl.createProductAvailability(productAvailabilityDTO)

            // Return a 200 with inside the warehouse information
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedWarehouse)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

    /**
     * PATCH /products/{productID}
     * Updates quantity of a product. Negative if order is issued or positive if a rollback operation
     * @param warehouseID: id of the warehouse
     * @param productID: id of the product to update the quantity
     * @param productAvailabilityDTO: quantity to update
     * @return the new product quantity in the warehouse
     */
    @PatchMapping("/{warehouseID}/products/{productID}")
    suspend fun updateProductAvailability(
        @PathVariable warehouseID: String,
        @PathVariable productID: String,
        @RequestHeader(name = "Authorization") jwtToken: String,
        @RequestBody @Valid productAvailabilityDTO: PartiallyProductAvailabilityDTO,
    ) : ResponseEntity<Mono<ProductAvailabilityDTO>> {

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            val updatedWarehouse = warehouseServiceImpl.updateProductAvailability(warehouseID,productID,productAvailabilityDTO)

            // Return a 200 with inside the warehouse information
            return ResponseEntity.status(HttpStatus.CREATED).body(updatedWarehouse)

        } catch (error: ErrorResponse) {
            // There was an error. Return an error message
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }

}