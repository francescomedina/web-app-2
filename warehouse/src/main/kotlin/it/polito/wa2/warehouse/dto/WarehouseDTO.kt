package it.polito.wa2.warehouse.dto

import org.bson.types.ObjectId

data class WarehouseDTO (
    val id: ObjectId? = null,

    val name: String = "",
    val region: String = "",
)