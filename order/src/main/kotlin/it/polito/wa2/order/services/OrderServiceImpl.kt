package it.polito.wa2.order.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.AppRuntimeException
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.order.domain.DeliveryEntity
import it.polito.wa2.order.domain.OrderEntity
import it.polito.wa2.order.domain.ProductEntity
import it.polito.wa2.order.dto.*
import it.polito.wa2.order.outbox.OutboxEventPublisher
import it.polito.wa2.order.repositories.OrderRepository
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.switchIfEmpty
import kotlin.math.log

@Service
@Transactional
class OrderServiceImpl(
    val orderRepository: OrderRepository,
    val eventPublisher: OutboxEventPublisher,
    val mailService: MailService,
) : OrderService {

    private val adminEmail = "marco.lg1997@gmail.com"
    private val logger = LoggerFactory.getLogger(OrderServiceImpl::class.java)

    fun saveOrder(order: OrderEntity, toDelete: Boolean = false): Mono<OrderDTO> {
        return Mono.just(order)
            .flatMap(orderRepository::save)
            .publishOn(Schedulers.boundedElastic())
            .doOnNext {
                eventPublisher.publish(
                    "order.topic",
                    it.id.toString(),
                    gson.toJson(it),
                    if (toDelete) "ORDER_CANCELED" else "ORDER_CREATED"
                ).subscribe()
            }
            .onErrorResume { Mono.error(AppRuntimeException(it.message, HttpStatus.INTERNAL_SERVER_ERROR, it)) }
            .map { it.toOrderDTO() }
    }

    /**
     * Create an order associated to a buyer
     * @param userInfoJWT : information about the user that make the request
     * @param orderDTO : information about the order
     * @return the order created
     */
    override fun createOrder(userInfoJWT: UserInfoJWT, orderDTO: OrderDTO): Mono<OrderDTO> {
        // Check if the buyer is the same user logged-in
        if (userInfoJWT.username == orderDTO.buyer) {

            val order = OrderEntity(
                buyer = userInfoJWT.username,
                status = "ISSUING",
                products = orderDTO.products?.map {
                    ProductEntity(it.id, it.quantity, it.price)
                }?.toList()
            )
            return saveOrder(order)
        }

        return Mono.error(ErrorResponse(HttpStatus.BAD_REQUEST, "You can't create order for another person"))
    }

    override fun deleteOrder(userInfoJWT: UserInfoJWT, orderId: ObjectId): Mono<Void> {
        return orderRepository.findById(orderId.toString())
            .switchIfEmpty {
                // If it is empty means that we don't have an order with such orderId
                logger.error("Asking to delete a non-existent order with id $orderId.")
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "Order does not exist")
            }
            .doOnNext { it ->
                // The order exists, let's see if it can be eliminated

                if (!userInfoJWT.isAdmin() && userInfoJWT.username != it?.buyer) {
                    // The order is not made by that user and the user is not an admin
                    logger.error("Asking to delete an order that is not the logged in user. OrderId: $orderId is made by ${it.buyer} and ${userInfoJWT.username} ask to eliminate.")
                    throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can't cancel orders for another person")
                }

                if (it.status != "ISSUED") {
                    // If the status is not ISSUED it cannot be eliminated anymore
                    logger.error("Asking to delete an order with orderId $orderId and status ${it.status}.")
                    throw ErrorResponse(
                        HttpStatus.BAD_REQUEST,
                        "You can cancel an order only if its status is ISSUED. That order is with status ${it.status}"
                    )
                }

                // Now we can modify the order. That will trigger 2 events:
                // 1. All the products inside the order are returned inside the warehouse
                // 2. The user is refunded

                saveOrder(it, true).doOnError { e ->
                    logger.error("Error during saveOrder on deleteOrder function. Error type: $e")
                    throw ErrorResponse(HttpStatus.BAD_REQUEST, "Error during returning item")
                }.doOnNext {

                    // We will email the buyer and to the admin saying that the item is cancelled
                    listOf(it.buyer, adminEmail).forEach { to ->
                        mailService.sendMessage(
                            to.toString(),
                            "Order ${it.id} CANCELED",
                            "Order ${it.id} was CANCELED by the User ${it.buyer}"
                        )
                    }
                    logger.info("DELETE ORDER COMPLETED. EMAIL SENT")
                }.subscribe()

            }
            .then()
    }

    override suspend fun updatePartiallyOrder(orderId: String, orderDTO: PartiallyOrderDTO): OrderDTO {
        // Check if the order exist
        val orderEntity = orderRepository.findById(orderId).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.BAD_REQUEST, "Order not found")

        // If the user specify the product we will update
        val prods: List<ProductEntity> = if (orderDTO.products != null) {
            orderDTO.products!!.map {
                ProductEntity(
                    id = it.id,
                    quantity = it.quantity,
                    price = it.price
                )
            }
        } else {
            orderEntity.products!!
        }

        logger.info("new prods $prods")

        // If the user specify the delivery we will update
        val delivery: List<DeliveryEntity> = if (orderDTO.delivery!=null) {
            orderDTO.delivery!!.map {
                DeliveryEntity(
                    shippingAddress = it.shippingAddress,
                    warehouseId = it.warehouseId,
                    products = prods
                )
            }
        } else {
            orderEntity.delivery!!
        }

        logger.info("new delivery $delivery")

        logger.info("ORDERDTO.STATUS ${orderDTO.status} ORDERENTITY ${orderEntity.status} ORDER DTO $orderDTO")

        val newOrder = OrderEntity(
            id = orderEntity.id,
            buyer = orderDTO.buyer ?: orderEntity.buyer,
            status = orderEntity.status, //TODO: Non è aggiornato orderDTO.status ?: orderEntity.status
            products = prods,
            delivery = delivery
        )

        logger.info("new order $newOrder")

        val savedNewOrder = orderRepository.save(newOrder).onErrorMap { error ->
            logger.error("Error during saving: $error")
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Error during saving: $error")
        }.awaitSingle()

        logger.info("DOPO SALVAREEEEE $savedNewOrder")

        // Send email to the buyer only if the order status changes
        if (orderEntity.status != newOrder.status) {
            listOf(newOrder.buyer, adminEmail).forEach { to ->
                mailService.sendMessage(
                    to.toString(),
                    "Order ${newOrder.id} ${newOrder.status}",
                    "Order was successfully ${newOrder.status}. User ${newOrder.buyer}"
                )
            }
        }

        logger.info("DOPO EMAIL")

        return savedNewOrder.toOrderDTO()
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

        val orders = orderRepository.findAllByBuyer(userInfoJWT.username)
        return orders.map { it.toOrderDTO() }
    }
}
