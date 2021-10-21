package it.polito.wa2.warehouse.services

import it.polito.wa2.api.core.warehouse.Warehouse
import it.polito.wa2.warehouse.persistence.WarehouseEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Mappings


@Mapper(componentModel = "spring")
interface WarehouseMapper {

    @Mappings(Mapping(target = "serviceAddress", ignore = true))
    fun entityToApi(entity: WarehouseEntity): Warehouse

    @Mappings(Mapping(target = "id", ignore = true))
    fun apiToEntity(api: Warehouse): WarehouseEntity
}

