package it.polito.wa2.warehouse.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import it.polito.wa2.warehouse.domain.WarehouseEntity
import it.polito.wa2.warehouse.dto.ProductAvailabilityDTO
import it.polito.wa2.warehouse.dto.WarehouseDTO
import it.polito.wa2.warehouse.dto.toWarehouseDTO
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface WarehouseService {
    fun getWarehouses(): Flux<WarehouseDTO>
    suspend fun getWarehouseByID(warehouseID: String): WarehouseDTO
    fun createWarehouse(warehouseDTO: WarehouseDTO): Mono<WarehouseDTO>
    fun updateWarehouse(warehouseID: String, warehouseDTO: WarehouseDTO): Mono<WarehouseDTO>
    suspend fun updatePartiallyWarehouse(warehouseID: String, warehouseDTO: WarehouseDTO): Mono<WarehouseDTO>
    suspend fun deleteWarehouse(warehouseID: String): Mono<Void>
    fun createProductAvailability(productAvailabilityDTO: ProductAvailabilityDTO): Mono<ProductAvailabilityDTO>
    suspend fun updateProductAvailability(productID: String, productAvailabilityDTO: ProductAvailabilityDTO): Mono<ProductAvailabilityDTO>
}