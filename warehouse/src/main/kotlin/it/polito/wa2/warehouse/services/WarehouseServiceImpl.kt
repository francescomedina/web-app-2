package it.polito.wa2.warehouse.services

import it.polito.wa2.warehouse.dto.WarehouseDTO
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

@Service
class WarehouseServiceImpl (


): WarehouseService  {
    override fun getWarehouses(): List<String> {
        TODO("Not yet implemented")
    }

    override fun getWarehouseByID(warehouseID: String): WarehouseDTO {
        TODO("Not yet implemented")
    }

    override fun createWarehouse(warehouseDTO: WarehouseDTO): WarehouseDTO {
        TODO("Not yet implemented")
    }

    override fun getWarehouseIdByProductID(productID: String): List<String> {
        TODO("Not yet implemented")
    }

}