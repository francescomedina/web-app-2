package it.polito.wa2.order.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.order.domain.OrderEntity
import it.polito.wa2.order.dto.*
import it.polito.wa2.order.outbox.OutboxEventPublisher
import it.polito.wa2.order.repositories.OrderRepository
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*

@Service
class OrderServiceImpl(
    val orderRepository: OrderRepository,
    val eventPublisher: OutboxEventPublisher
) : OrderService {

    /**
     * Create a order associated to a username
     * @param userInfoJWT : information about the user that make the request
     * @param username : username associated to that order
     * @return the order created
     */
    override suspend fun createOrder(userInfoJWT: UserInfoJWT, buyerId: String): Mono<OrderDTO> {

        if (userInfoJWT.username == buyerId) {
            val order = OrderEntity(buyer = userInfoJWT.username)


            val orderCreated = orderRepository.save(order).onErrorMap {
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "ORDER NOT CREATED")
            }.awaitSingleOrNull()
            orderCreated?.let {
                eventPublisher.publish(
                    "order.topic",
                    orderCreated.id.toString(),
                    orderCreated.toString(),
                    "ORDER_CREATED"
                )
                return mono { it.toOrderDTO() }
            }
        }

        throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can't create order for another person")

    }

    override suspend fun deleteOrder(userInfoJWT: UserInfoJWT, orderId: ObjectId): Mono<Void> {

        val order = orderRepository.findById(orderId.toString()).awaitSingleOrNull()
        if (userInfoJWT.username == order?.buyer) {
            val orderEntity = OrderEntity(id = orderId)

            return orderRepository.delete(orderEntity).map {
//                exampleEventService.publishEvent(ExampleEvent(order.id.toString(), "ORDER_DELETED"))
                it
            }
        }

        throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can't create order for another person")
    }

    override fun updateOrder(userInfoJWT: UserInfoJWT, username: String): Mono<OrderDTO> {
        TODO("Not yet implemented")
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

        val orders = orderRepository.findAll()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Order not found")

        if (userInfoJWT.isAdmin()) {
            return orders.map { it.toOrderDTO() }
        }

        throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see all orders")
    }
}

//import it.polito.wa2.api.core.order.Order
//import it.polito.wa2.api.core.order.OrderService
//import it.polito.wa2.api.exceptions.InvalidInputException
//import it.polito.wa2.api.exceptions.NotFoundException
//import it.polito.wa2.order.persistence.OrderEntity
//import it.polito.wa2.order.persistence.OrderRepository
//import it.polito.wa2.util.http.ServiceUtil
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration
//import org.springframework.dao.DuplicateKeyException
//import org.springframework.web.bind.annotation.RestController
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//import java.util.logging.Level
//
////@RestController
//@EnableAutoConfiguration
//class OrderServiceImpl @Autowired constructor(
//    repository: OrderRepository,
//    mapper: OrderMapper,
//    serviceUtil: ServiceUtil
//) : OrderService {
//    private val serviceUtil: ServiceUtil
//    private val repository: OrderRepository
//    private val mapper: OrderMapper
//
//    override fun deleteOrder(orderId: Int): Mono<Void?>? {
//        if (orderId < 1) {
//            throw InvalidInputException("Invalid orderId: $orderId")
//        }
//        LOG.debug("deleteProduct: tries to delete an entity with orderId: {}", orderId)
//        return repository.findByOrderId(orderId)
//            .log(LOG.name, Level.FINE)
//            .mapNotNull { e -> e?.let { repository.delete(it) } }
//            .flatMap { e -> e }
//    }
//
//    override fun putOrder(body: Order?): Mono<Order?>? {
//        TODO("Not yet implemented")
//    }
//
//    private fun setServiceAddress(e: Order): Order {
//        e.serviceAddress = serviceUtil.serviceAddress.toString()
//        return e
//    }
//
//    companion object {
//        private val LOG = LoggerFactory.getLogger(OrderServiceImpl::class.java)
//    }
//
//    init {
//        this.repository = repository
//        this.mapper = mapper
//        this.serviceUtil = serviceUtil
//    }
//
//    override fun persistOrder(body: Order?): Order? {
//        TODO("Not yet implemented")
//    }
//
////    override fun createOrder(body: Order): Mono<Order?>? {
////        if (body != null) {
////            if (body.orderId < 1) {
////                throw InvalidInputException("Invalid orderId: " + body.orderId)
////            }
////        }
////        val entity: OrderEntity = mapper.apiToEntity(body)
////
////        if (body != null) {
////            return repository.save(entity)
////                .log(LOG.name, Level.FINE)
////                .onErrorMap(DuplicateKeyException::class.java) { ex -> InvalidInputException("Duplicate key, Product Id: " + body.orderId)
////                }
////                .mapNotNull { e -> mapper.entityToApi(e) }
////        }
////        return null
////    }
//
//    override fun createOrder(body: Order?): Mono<Order?>? {
//        if (body != null) {
//            if (body.orderId < 1) {
//                throw InvalidInputException("Invalid orderId: " + body.orderId)
//            }
//        }
//        val entity: OrderEntity = body?.let { mapper.apiToEntity(it) }!!
//
//        if (body != null) {
//            return repository.save(entity)
//                .log(LOG.name, Level.FINE)
//                .onErrorMap(DuplicateKeyException::class.java) { ex -> InvalidInputException("Duplicate key, Product Id: " + body.orderId)
//                }
//                .mapNotNull { e -> mapper.entityToApi(e) }
//        }
//        return null
//    }
//
//    override fun getOrder(orderId: Int): Mono<Order?>? {
//        if (orderId < 1) {
//            throw InvalidInputException("Invalid orderId: $orderId")
//        }
//        LOG.info("Will get product info for id={}", orderId)
//
//        return repository.findByOrderId(orderId)
//            .switchIfEmpty(Mono.error(NotFoundException("No product found for orderId: $orderId")))
//            .log(LOG.name, Level.FINE)
//            .mapNotNull { e -> e?.let { mapper.entityToApi(it) } }
//            .mapNotNull { e -> e?.let { setServiceAddress(it) } }
//    }
//
//    override fun getOrders(): Flux<Order?>? {
//        TODO("Not yet implemented")
//    }
//
//    override fun updateStatus(order: Order, status: String) {
//        TODO("Not yet implemented")
//    }
//}
