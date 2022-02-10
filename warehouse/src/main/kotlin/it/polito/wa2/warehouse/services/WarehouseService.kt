package it.polito.wa2.warehouse.services

import it.polito.wa2.warehouse.dto.ProductAvailabilityDTO
import it.polito.wa2.warehouse.dto.WarehouseDTO
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface WarehouseService {
    fun getWarehouses(): Flux<WarehouseDTO>
    suspend fun getWarehouseByID(warehouseID: String): WarehouseDTO
    fun createWarehouse(warehouseDTO: WarehouseDTO): Mono<WarehouseDTO>
    fun updateWarehouse(warehouseID: String, warehouseDTO: WarehouseDTO): Mono<WarehouseDTO>
    suspend fun updatePartiallyWarehouse(warehouseID: String, warehouseDTO: PartiallyWarehouseDTO): Mono<WarehouseDTO>
    suspend fun deleteWarehouse(warehouseID: String): Mono<Void>
    fun getProductAvailabilityByProductId(productID: ObjectId): Flux<ProductAvailabilityDTO>
    fun createProductAvailability(productAvailabilityDTO: ProductAvailabilityDTO): Mono<ProductAvailabilityDTO>
    suspend fun updateProductAvailability(warehouseID: String,productID: String, productAvailabilityDTO: PartiallyProductAvailabilityDTO): Mono<ProductAvailabilityDTO>
}