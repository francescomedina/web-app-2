package it.polito.wa2.warehouse.dto

import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import org.bson.types.ObjectId
import javax.validation.constraints.DecimalMin
import javax.validation.constraints.NotNull

data class ProductAvailabilityDTO(
    val id: ObjectId? = null,
    @field:NotNull(message ="Minimum quantity is required")
    @field:DecimalMin(value = "0.0", message = "Minimum quantity must be positive")
    val min_quantity: Int?,
    @field:NotNull(message ="ProductId is required")
    val productId: ObjectId?,
    @field:NotNull(message ="WarehouseId is required")
    val warehouseId: ObjectId?,
    @field:NotNull(message ="Price is required")
    @field:DecimalMin(value = "0.0", message = "Quantity must be positive")
    val quantity: Int?
)

fun ProductAvailabilityEntity.toProductAvailabilityDTO(): ProductAvailabilityDTO {
    return ProductAvailabilityDTO(
        id = id,
        min_quantity = min_quantity,
        productId = productId,
        warehouseId = warehouseId,
        quantity = quantity
    )
}
