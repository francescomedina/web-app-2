package it.polito.wa2.order.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.order.domain.OrderEntity
import it.polito.wa2.order.dto.OrderDTO
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

interface OrderService {
    suspend fun createOrder(userInfoJWT: UserInfoJWT, buyerId: String): Mono<OrderDTO>
    suspend fun deleteOrder(userInfoJWT: UserInfoJWT, orderId: ObjectId): Mono<Void>
    fun updateOrder(userInfoJWT: UserInfoJWT, username: String): Mono<OrderDTO>
    suspend fun getOrderById(userInfoJWT: UserInfoJWT, orderId: ObjectId): OrderDTO
    suspend fun getOrders(userInfoJWT: UserInfoJWT): Flux<OrderDTO>
}