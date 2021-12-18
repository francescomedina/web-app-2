package it.polito.wa2.warehouse.services

import it.polito.wa2.warehouse.dto.WarehouseDTO

interface WarehouseService {
    fun getWarehouses(): List<String>
    fun getWarehouseByID(warehouseID: String): WarehouseDTO
    fun createWarehouse(warehouseDTO: WarehouseDTO): WarehouseDTO
}