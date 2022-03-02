package it.polito.wa2.warehouse.services

import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.warehouse.domain.*
import it.polito.wa2.warehouse.dto.*
import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
import it.polito.wa2.warehouse.repository.WarehouseRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.Min
import javax.validation.constraints.NotNull

data class PartiallyWarehouseDTO (
    val id: ObjectId? = null,
    val name: String? = null,
    val region: String? = null,
)

data class PartiallyProductAvailabilityDTO(
    val id: ObjectId? = null,
    @field:DecimalMin(value = "0.0", message="Min Quantity must be positive")
    val min_quantity: Int?,
    val productId: ObjectId?,
    val warehouseId: ObjectId?,
    @field:DecimalMin(value = "0.0", message="Quantity must be positive")
    val quantity: Int?
)

@Service
class WarehouseServiceImpl (
    val warehouseRepository: WarehouseRepository,
    val productAvailabilityRepository: ProductAvailabilityRepository
): WarehouseService  {

    override fun getWarehouses(): Flux<WarehouseDTO> {
        return warehouseRepository.findAll().map {
            it.toWarehouseDTO()
        }
    }

    override suspend fun getWarehouseByID(warehouseID: String): WarehouseDTO {
        return warehouseRepository.findById(warehouseID).awaitSingleOrNull()?.toWarehouseDTO()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Warehouse not found")
    }

    override fun createWarehouse(warehouseDTO: WarehouseDTO): Mono<WarehouseDTO> {
        return warehouseRepository.save(WarehouseEntity(
            name = warehouseDTO.name,
            region = warehouseDTO.region
        )).map {
            it.toWarehouseDTO()
        }
    }

    override fun updateWarehouse(warehouseID: String, warehouseDTO: WarehouseDTO): Mono<WarehouseDTO> {
        return warehouseRepository.save(WarehouseEntity(
            id = ObjectId(warehouseID),
            name = warehouseDTO.name,
            region = warehouseDTO.region
        )).map {
            it.toWarehouseDTO()
        }
    }

    override suspend fun updatePartiallyWarehouse(warehouseID: String, warehouseDTO: PartiallyWarehouseDTO): Mono<WarehouseDTO>{
        val warehouse = warehouseRepository.findById(warehouseID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Warehouse not found")

        val updateWarehouse = WarehouseEntity(
            id = ObjectId(warehouseID),
            name = warehouseDTO.name ?: warehouse.name,
            region = warehouseDTO.region ?: warehouse.region,
        )

        return warehouseRepository.save(updateWarehouse).map {
            it.toWarehouseDTO()
        }
    }

    override suspend fun deleteWarehouse(warehouseID: String): Mono<Void> {
        val product = warehouseRepository.findById(warehouseID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Warehouse not found")

        return warehouseRepository.delete(product)
    }

    override fun getProductAvailabilityByProductId(productID: ObjectId): Flux<ProductAvailabilityDTO> {
        return productAvailabilityRepository.findAllByProductId(productID).map {
            it.toProductAvailabilityDTO()
        }
    }

    override fun createProductAvailability(productAvailabilityDTO: ProductAvailabilityDTO): Mono<ProductAvailabilityDTO>{
        return productAvailabilityRepository.save(ProductAvailabilityEntity(
            productId = productAvailabilityDTO.productId!!,
            quantity = productAvailabilityDTO.quantity!!,
            min_quantity = productAvailabilityDTO.min_quantity!!,
            warehouseId = productAvailabilityDTO.warehouseId!!
        )).map {
            it.toProductAvailabilityDTO()
        }
    }

    override suspend fun updateProductAvailability(warehouseID: String,productID: String, productAvailabilityDTO: PartiallyProductAvailabilityDTO): Mono<ProductAvailabilityDTO> {
        val pa = productAvailabilityRepository.findOneByWarehouseIdAndProductId(ObjectId(warehouseID),ObjectId(productID)).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Product availability not found")
        return productAvailabilityRepository.save(ProductAvailabilityEntity(
            id = pa.id,
            productId = productAvailabilityDTO.productId ?: pa.productId,
            quantity = productAvailabilityDTO.quantity ?: pa.quantity,
            min_quantity = productAvailabilityDTO.min_quantity ?: pa.min_quantity,
            warehouseId = productAvailabilityDTO.warehouseId ?: pa.warehouseId
        )).map {
            it.toProductAvailabilityDTO()
        }
    }
}