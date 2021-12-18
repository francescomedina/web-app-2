//package it.polito.wa2.catalog.services
//
//
//import it.polito.wa2.api.composite.catalog.CatalogCompositeService
//import it.polito.wa2.api.core.order.Order
//import it.polito.wa2.api.exceptions.NotFoundException
//import org.slf4j.Logger
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.web.bind.annotation.RestController
//import reactor.core.publisher.Mono
//import java.util.function.Consumer
//import java.util.function.Function
//import java.util.logging.Level
//
//
//@RestController
//class CatalogServiceImpl @Autowired constructor(
//    private val integration: CatalogIntegration
//) :
//    CatalogCompositeService {
//
//    private val LOG: Logger = LoggerFactory.getLogger(CatalogServiceImpl::class.java)
//
//    override fun createOrder(body: Order?): Mono<Void?>? {
//        return try {
//            val monoList: MutableList<Mono<*>> = ArrayList()
//            if(body != null){
//                LOG.debug("createCompositeProduct: creates a new composite entity for productId: {}", body.orderId)
//                val order = Order(
//                    body.status,
//                    body.orderId,
//                    body.price,
//                    body.buyer,
//                    body.amount,
//                    body.serviceAddress
//                )
//                integration.createOrder(order)?.let { monoList.add(it) }
//                LOG.debug("createCompositeProduct: composite entities created for productId: {}", body.orderId)
//            }
//            Mono.zip({ r: Array<Any?>? -> "" }, *monoList.toTypedArray())
//                .doOnError { ex: Throwable ->
//                    LOG.warn(
//                        "createCompositeProduct failed: {}",
//                        ex.toString()
//                    )
//                }
//                .then()
//        } catch (re: RuntimeException) {
//            LOG.warn(
//                "createCompositeProduct failed: {}",
//                re.toString()
//            )
//            throw re
//        }
//    }
//
//
//    override fun getOrder(orderId: Int): Mono<Order?>? {
//        val order = integration.getOrder(orderId)
//            ?: throw NotFoundException("No product found for productId: $orderId")
//        return order
//    }
//
//    override fun deleteOrder(orderId: Int): Mono<Void?>? {
//        return try {
//            LOG.debug("deleteCompositeProduct: Deletes a product aggregate for productId: {}", orderId)
//            Mono.zip(
//                Function { r: Array<Any?>? -> "" },
//                integration.deleteOrder(orderId)
//            )
//                .doOnError(Consumer { ex: Throwable ->
//                    LOG.warn("delete failed: {}", ex.toString())
//                })
//                .log(
//                    LOG.getName(), Level.FINE).then()
//        } catch (re: RuntimeException) {
//            LOG.warn("deleteCompositeProduct failed: {}", re.toString())
//            throw re
//        }
//    }
//}