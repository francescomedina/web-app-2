package it.polito.wa2.warehouse.controllers

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.util.jwt.JwtValidateUtils
import it.polito.wa2.warehouse.domain.WarehouseEntity
import it.polito.wa2.warehouse.dto.ProductDTO
import it.polito.wa2.warehouse.dto.WarehouseDTO
import it.polito.wa2.warehouse.services.ProductServiceImpl
import it.polito.wa2.warehouse.services.WarehouseServiceImpl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
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
    @GetMapping("/warehouses")
    suspend fun getWarehouses(
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<List<String>>{
        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the list of warehouses to the service
            val warehousesID: List<String> = warehouseServiceImpl.getWarehouses()

            // Return a 200 with inside list of all warehouses
            return ResponseEntity.status(HttpStatus.CREATED).body(warehousesID)

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
        @RequestHeader(name = "Authorization") jwtToken: String
    ): ResponseEntity<WarehouseDTO> {
        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

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
    @PostMapping("/warehouses")
    suspend fun createWarehouse(
        @RequestBody @Valid warehouseDTO: WarehouseDTO,
        @RequestHeader(name = "Authorization") jwtToken: String
    ) : ResponseEntity<WarehouseDTO>{

        try {
            // Extract userInfo from JWT
            val userInfoJWT: UserInfoJWT = jwtUtils.getDetailsFromJwtToken(jwtToken)

            // If the user is not an admin, we will return an error
            if (!userInfoJWT.isAdmin()) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied")
            }

            // Ask the service to create a new warehouse
            val createdWarehouse: WarehouseDTO = warehouseServiceImpl.createWarehouse(warehouseDTO)

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
    //TODO

    /**
     * PATCH /warehouses/{warehouseID}
     * Updates an existing warehouse (partial representation)
     * @param warehouseID: id of the warehouse to update
     * @param warehouseDTO:
     * @return the warehouse updated
     */
    //TODO

    /**
     * DELETE /warehouses/{warehouseID}
     * Deletes a warehouse
     * @param warehouseID: id of the warehouse to update
     * @return the warehouse deleted
     */
    //TODO

    /**
     * POST /warehouses/products
     * Add a product with a given initial quantity to the warehouse
     * @param productAvailabilityDTO:
     * @return the new product with quantity
     */
    //TODO

    /**
     * PATCH /warehouses/products/{productsID}
     * Updates quantity of a product. Negative if order is issued or positive if a rollback operation
     * @param productsID: id of the product to update the quantity
     * @param productAvailabilityDTO: quantity to update
     * @return the new product quantity in the warehouse
     */
    //TODO
}