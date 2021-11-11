package it.polito.wa2.order.services

import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.core.order.OrderService
import it.polito.wa2.api.exceptions.InvalidInputException
import it.polito.wa2.api.exceptions.NotFoundException
import it.polito.wa2.order.persistence.OrderEntity
import it.polito.wa2.order.persistence.OrderRepository
import it.polito.wa2.util.http.ServiceUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.dao.DuplicateKeyException
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import java.util.logging.Level

//@RestController
@EnableAutoConfiguration
class OrderServiceImpl @Autowired constructor(
    repository: OrderRepository,
    mapper: OrderMapper,
    serviceUtil: ServiceUtil
) : OrderService {
    private val serviceUtil: ServiceUtil
    private val repository: OrderRepository
    private val mapper: OrderMapper

    override fun deleteOrder(orderId: Int): Mono<Void?>? {
        if (orderId < 1) {
            throw InvalidInputException("Invalid orderId: $orderId")
        }
        LOG.debug("deleteProduct: tries to delete an entity with orderId: {}", orderId)
        return repository.findByOrderId(orderId)
            .log(LOG.name, Level.FINE)
            .mapNotNull { e -> e?.let { repository.delete(it) } }
            .flatMap { e -> e }
    }

    private fun setServiceAddress(e: Order): Order {
        e.serviceAddress = serviceUtil.serviceAddress.toString()
        return e
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OrderServiceImpl::class.java)
    }

    init {
        this.repository = repository
        this.mapper = mapper
        this.serviceUtil = serviceUtil
    }

    override fun persistOrder(body: Order?): Order? {
        TODO("Not yet implemented")
    }

//    override fun createOrder(body: Order): Mono<Order?>? {
//        if (body != null) {
//            if (body.orderId < 1) {
//                throw InvalidInputException("Invalid orderId: " + body.orderId)
//            }
//        }
//        val entity: OrderEntity = mapper.apiToEntity(body)
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

    override fun createOrder(body: Order?): Mono<Order?>? {
        if (body != null) {
            if (body.orderId < 1) {
                throw InvalidInputException("Invalid orderId: " + body.orderId)
            }
        }
        val entity: OrderEntity = body?.let { mapper.apiToEntity(it) }!!

        return repository.save(entity)
            .log(LOG.name, Level.FINE)
            .onErrorMap(DuplicateKeyException::class.java) { ex ->
                InvalidInputException("Duplicate key, Product Id: " + body.orderId)
            }
            .mapNotNull { e -> mapper.entityToApi(e) }

    }

    override fun getOrder(orderId: Int): Mono<Order?>? {
        if (orderId < 1) {
            throw InvalidInputException("Invalid orderId: $orderId")
        }
        LOG.info("Will get product info for id={}", orderId)

        return repository.findByOrderId(orderId)
            .switchIfEmpty(Mono.error(NotFoundException("No product found for orderId: $orderId")))
            .log(LOG.name, Level.FINE)
            .mapNotNull { e -> e?.let { mapper.entityToApi(it) } }
            .mapNotNull { e -> e?.let { setServiceAddress(it) } }
    }

    override fun updateStatus(order: Order, status: String) {
        TODO("Not yet implemented")
    }
}