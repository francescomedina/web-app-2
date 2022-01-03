package it.polito.wa2.order.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.order.domain.OrderEntity
import it.polito.wa2.order.domain.ProductEntity
import it.polito.wa2.order.dto.*
import it.polito.wa2.order.outbox.OutboxEvent
import it.polito.wa2.order.outbox.OutboxEventPublisher
import it.polito.wa2.order.repositories.OrderRepository
import it.polito.wa2.order.utils.ObjectIdTypeAdapter
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Service
@EnableTransactionManagement
class OrderServiceImpl(
    val orderRepository: OrderRepository,
    val eventPublisher: OutboxEventPublisher,
    val mailService: MailService,
    val transactionalOperator: TransactionalOperator
) : OrderService {

    private val logger = LoggerFactory.getLogger(OrderServiceImpl::class.java)

    @Transactional
    fun saveOrder(order: OrderEntity) : Flux<OutboxEvent> {
        val tx = Flux.just(order)
            .flatMap(orderRepository::save)
            .flatMap {
                val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
                eventPublisher.publish(
                    "order.topic",
                    it.id.toString(),
                    gson.toJson(it),
                    "ORDER_CREATED"
                )
            }
        return transactionalOperator.execute { tx }
    }

    /**
     * Create a order associated to a username
     * @param userInfoJWT : information about the user that make the request
     * @param username : username associated to that order
     * @return the order created
     */
    override suspend fun createOrder(userInfoJWT: UserInfoJWT, orderDTO: OrderDTO): Mono<OrderDTO> {
        if (userInfoJWT.username == orderDTO.buyer) {
            val order = OrderEntity(
                buyer = userInfoJWT.username,
                status = "ISSUING",
                products = orderDTO.products?.map {
                    ProductEntity(it.id,it.quantity,it.price)
                }?.toList()
            )
            saveOrder(order).subscribe()
        }

        throw ErrorResponse(HttpStatus.BAD_REQUEST, "PROVA You can't create order for another person")

    }

    override suspend fun deleteOrder(userInfoJWT: UserInfoJWT, orderId: ObjectId): Mono<Void> {
        val order = orderRepository.findById(orderId.toString()).onErrorMap {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Order does not exist")
        }.awaitSingleOrNull()
        if (userInfoJWT.username == order?.buyer) {
            if(order.status == "ISSUED"){
                logger.info("ORDER ENTRATO: $order")
                order.status = "CANCELING"
                val orderUpdated = orderRepository.save(order).onErrorMap {
                    throw ErrorResponse(HttpStatus.BAD_REQUEST, "ORDER NOT DELETED")
                }.awaitSingle()
                logger.info("ORDER Received: $orderUpdated")
                val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
                orderUpdated?.let {
                    eventPublisher.publish(
                        "order.topic",
                        order.id.toString(),
                        gson.toJson(orderUpdated),
                        "ORDER_CANCELED"
                    )
                }
                return mono { null }
            }
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can cancel an order only if its status is ISSUED")
        }
        throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can't cancel orders for another person")
    }

    override suspend fun updateOrder(userInfoJWT: UserInfoJWT?, orderId: String, orderDTO: OrderDTO, username: String?, trusted: Boolean): Mono<OrderDTO> {

        if (trusted || userInfoJWT!!.isAdmin()) {
            val orderEntity = orderRepository.findById(orderId).onErrorMap {
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "Order not found")
            }.awaitSingleOrNull()

            orderEntity?.let {
                orderEntity.status = orderDTO.status
                ///TODO MEttere i prodotti anche
                orderRepository.save(it).onErrorMap { error ->
                    throw ErrorResponse(HttpStatus.BAD_REQUEST, error.message ?: "Generic error")
                }.awaitSingle()
                if(orderDTO.status == "ISSUED"){
                    mailService.sendMessage(it.buyer!!, "Order Issued", "Order was successfully issued")
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
