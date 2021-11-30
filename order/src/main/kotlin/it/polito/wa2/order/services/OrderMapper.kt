//package it.polito.wa2.order.services
//
//import it.polito.wa2.api.core.order.Order
//import it.polito.wa2.order.persistence.OrderEntity
//import org.mapstruct.Mapper
//import org.mapstruct.Mapping
//import org.mapstruct.Mappings
//
//
//@Mapper(componentModel = "spring")
//interface OrderMapper {
//
//    @Mappings(Mapping(target = "serviceAddress", ignore = true))
//    fun entityToApi(entity: OrderEntity): Order
//
//    @Mappings(Mapping(target = "id", ignore = true))
//    fun apiToEntity(api: Order): OrderEntity
//}
//
