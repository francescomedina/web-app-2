package it.polito.wa2.warehouse.dto

import it.polito.wa2.warehouse.domain.WarehouseEntity
import org.bson.types.ObjectId
import javax.validation.constraints.NotBlank

data class WarehouseDTO (
    val id: ObjectId? = null,

    @field:NotBlank(message = "Name is required")
    val name: String = "",
    @field:NotBlank(message = "Region is required")
    val region: String = "",
)

fun WarehouseEntity.toWarehouseDTO(): WarehouseDTO {
    return WarehouseDTO(
        id = id,
        name = name,
        region = region,
    )
}
