package it.polito.wa2.warehouse.dto

import it.polito.wa2.warehouse.domain.WarehouseEntity
import org.bson.types.ObjectId

data class WarehouseDTO (
    val id: ObjectId? = null,

    val name: String? = "",
    val region: String? = "",
    val products: List<ProductAvailabilityDTO>?
)

fun WarehouseEntity.toWarehouseDTO(): WarehouseDTO {
    return WarehouseDTO(
        id = id,
        name = name,
        region = region,
        products = products?.map { it.toProductAvailabilityDTO() }
    )
}
