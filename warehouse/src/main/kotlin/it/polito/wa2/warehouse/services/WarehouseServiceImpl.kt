package it.polito.wa2.warehouse.services

import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.warehouse.domain.*
import it.polito.wa2.warehouse.dto.*
import it.polito.wa2.warehouse.repository.WarehouseRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class WarehouseServiceImpl (
    val warehouseRepository: WarehouseRepository,
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
            name = warehouseDTO.name!!,
            region = warehouseDTO.region!!,
            products = null
        )).map {
            it.toWarehouseDTO()
        }
    }

    override fun getWarehouseIdByProductID(productID: String): Flux<WarehouseDTO> {
        return warehouseRepository.findAllByProductsProductId(productID).map {
            it.toWarehouseDTO()
        }?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Warehouse not found")
    }

    override fun updateWarehouse(warehouseID: String, warehouseDTO: WarehouseDTO): Mono<WarehouseDTO> {
        return warehouseRepository.save(WarehouseEntity(
            id = ObjectId(warehouseID),
            name = warehouseDTO.name!!,
            region = warehouseDTO.region!!,
            products = null
        )).map {
            it.toWarehouseDTO()
        }?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Warehouse not updated")
    }

    override suspend fun updatePartiallyWarehouse(warehouseID: String, warehouseDTO: WarehouseDTO): Mono<WarehouseDTO>{
        val warehouse = warehouseRepository.findById(warehouseID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Warehouse not found")

        val updateWarehouse = WarehouseEntity(
            id = ObjectId(warehouseID),
            name = warehouseDTO.name ?: warehouse.name,
            region = warehouseDTO.region ?: warehouse.region,
            products = warehouseDTO.products?.map {
                ProductAvailabilityEntity(
                    id = ObjectId(it.id.toString()),
                    it.productId,
                    it.quantity,
                    it.min_quantity,
                )
            } ?: warehouse.products
        )

        return warehouseRepository.save(updateWarehouse).map {
            it.toWarehouseDTO()
        }
    }

    override suspend fun deleteWarehouse(warehouseID: String): Mono<Void> {
        val product = warehouseRepository.findById(warehouseID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Product not found")

        return warehouseRepository.delete(product)
    }

    override suspend fun addProductAvailability(warehouseID: String, productAvailabilityDTO: ProductAvailabilityDTO): Mono<WarehouseDTO>{
        val warehouse = warehouseRepository.findById(warehouseID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Warehouse not found")

        val newProducts = mutableListOf<ProductAvailabilityEntity>()
        if(warehouse.products != null){
            newProducts.addAll(warehouse.products!!)
        }
        newProducts.add(
            ProductAvailabilityEntity(
                productId = productAvailabilityDTO.productId,
                quantity = productAvailabilityDTO.quantity,
                min_quantity = productAvailabilityDTO.min_quantity,
        ))
        warehouse.products = newProducts.toList()
        return warehouseRepository.save(warehouse).map {
            it.toWarehouseDTO()
        }
    }

    override suspend fun updateProductAvailability(warehouseID: String, productAvailabilityDTO: ProductAvailabilityDTO): Mono<WarehouseDTO> {
        val warehouse = warehouseRepository.findById(warehouseID).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Warehouse not found")

        val newProducts = mutableListOf<ProductAvailabilityEntity>()
        if(warehouse.products != null){
            newProducts.addAll(warehouse.products!!)
        }
        newProducts.map {
            if(it.id == productAvailabilityDTO.id && it.productId == productAvailabilityDTO.productId){
                productAvailabilityDTO
            }else{
                it
            }
        }
        warehouse.products = newProducts.toList()
        return warehouseRepository.save(warehouse).map {
            it.toWarehouseDTO()
        }
    }
}