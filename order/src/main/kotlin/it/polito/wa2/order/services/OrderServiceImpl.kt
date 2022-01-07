package it.polito.wa2.order.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.AppRuntimeException
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.order.domain.OrderEntity
import it.polito.wa2.order.domain.ProductEntity
import it.polito.wa2.order.dto.*
import it.polito.wa2.order.outbox.OutboxEventPublisher
import it.polito.wa2.order.repositories.OrderRepository
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Service
@Transactional
class OrderServiceImpl(
    val orderRepository: OrderRepository,
    val eventPublisher: OutboxEventPublisher,
    val mailService: MailService,
) : OrderService {

    private val logger = LoggerFactory.getLogger(OrderServiceImpl::class.java)
    private val adminEmail = "admin@test.com"

    /**
     * TRANSACTIONAL: changes are committed if no exceptions are generated, rollback otherwise
     */
    fun saveOrder(order: OrderEntity, toDelete: Boolean = false) : Mono<OrderDTO> {
        return Mono.just(order)
            .flatMap(orderRepository::save)
            .doOnNext {
                eventPublisher.publish(
                    "order.topic",
                    it.id.toString(),
                    gson.toJson(it),
                    if(toDelete) "ORDER_CANCELED" else "ORDER_CREATED"
                ).subscribe()
            }
            .onErrorResume { Mono.error(AppRuntimeException(it.message, HttpStatus.INTERNAL_SERVER_ERROR,it)) }
            .map { it.toOrderDTO() }
    }

    /**
     * Create a order associated to a username
     * @param userInfoJWT : information about the user that make the request
     * @param username : username associated to that order
     * @return the order created
     */
    override fun createOrder(userInfoJWT: UserInfoJWT, orderDTO: OrderDTO): Mono<OrderDTO> {
        if (userInfoJWT.username == orderDTO.buyer) {
            val order = OrderEntity(
                buyer = userInfoJWT.username,
                status = "ISSUING",
                products = orderDTO.products?.map {
                    ProductEntity(it.id,it.quantity,it.price)
                }?.toList()
            )
            return saveOrder(order)
        }
        throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can't create order for another person")
    }

    override fun deleteOrder(userInfoJWT: UserInfoJWT, orderId: ObjectId): Mono<Void> {
        return orderRepository.findById(orderId.toString())
            .doOnNext {
                if (userInfoJWT.username != it?.buyer) {
                    throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can't cancel orders for another person")
                }
                if(it.status != "ISSUED"){
                    throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can cancel an order only if its status is ISSUED")
                }
                saveOrder(it,true).subscribe()
            }
            .onErrorMap {
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "Order does not exist")
            }
            .then()
    }

    override suspend fun updateOrder(userInfoJWT: UserInfoJWT?, orderId: String, orderDTO: OrderDTO, username: String?, trusted: Boolean): Mono<OrderDTO> {
        if (trusted || userInfoJWT!!.isAdmin()) {
            val orderEntity = orderRepository.findById(orderId).onErrorMap {
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "Order not found")
            }.awaitSingleOrNull()

            orderEntity?.let {
                val prevStatus = orderEntity.status
                orderEntity.status = orderDTO.status
                orderRepository.save(it).onErrorMap { error ->
                    throw ErrorResponse(HttpStatus.BAD_REQUEST, error.message ?: "Generic error")
                }.awaitSingle()
                if(prevStatus != orderDTO.status){
                    listOf(it.buyer, adminEmail).forEach { to ->
                        mailService.sendMessage(to.toString(), "Order ${orderDTO.id.toString()} ${orderDTO.status}", "Order was successfully ${orderDTO.status}. User ${orderDTO.buyer}")
                    }
                }
                return mono { it.toOrderDTO() }
            }

            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Order does not exist")
        }
        throw ErrorResponse(HttpStatus.BAD_REQUEST, "User not authenticated")
    }

    /**
     * Retrieve the order by the OrderId
     * @param userInfoJWT : information about the user that make the request
     * @param orderId : id of the order
     * @return the order information
     */
    override suspend fun getOrderById(userInfoJWT: UserInfoJWT, orderId: ObjectId): OrderDTO {

        val order = orderRepository.findById(orderId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Order not found")

        if (order.buyer == userInfoJWT.username || userInfoJWT.isAdmin()) {
            return order.toOrderDTO()
        }

        throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see this order")
    }

    override suspend fun getOrders(userInfoJWT: UserInfoJWT): Flux<OrderDTO> {

        if (userInfoJWT.isAdmin()) {
            val orders = orderRepository.findAll()
                ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Order not found")
            return orders.map { it.toOrderDTO() }
        }

        throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see all orders")
    }
}
