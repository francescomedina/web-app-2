package it.polito.wa2.order.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.order.dto.OrderDTO
import it.polito.wa2.order.dto.PartiallyOrderDTO
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

interface OrderService {
    fun createOrder(userInfoJWT: UserInfoJWT, orderDTO: OrderDTO): Mono<OrderDTO>
    fun deleteOrder(userInfoJWT: UserInfoJWT, orderId: ObjectId): Mono<Void>
    suspend fun updatePartiallyOrder(orderId: String, orderDTO: PartiallyOrderDTO): OrderDTO
    suspend fun getOrderById(userInfoJWT: UserInfoJWT, orderId: ObjectId): OrderDTO
    suspend fun getOrders(userInfoJWT: UserInfoJWT): Flux<OrderDTO>
}