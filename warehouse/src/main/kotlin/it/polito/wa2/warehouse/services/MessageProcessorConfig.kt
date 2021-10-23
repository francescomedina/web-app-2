package it.polito.wa2.warehouse.services

import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.core.warehouse.Warehouse
import it.polito.wa2.api.core.warehouse.WarehouseService
import it.polito.wa2.api.event.Event
import it.polito.wa2.api.exceptions.EventProcessingException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.function.Consumer

@EnableAutoConfiguration
@Component
class MessageProcessorConfig @Autowired constructor(warehouseService: WarehouseService) {
    private val warehouseService: WarehouseService

    @Bean
    fun messageProcessor(): Consumer<Event<Int?, Order?>> {
        return Consumer<Event<Int?, Order?>> { event: Event<Int?, Order?> ->
            LOG.info("WAREHOUSE SERVICE: Process message created at {}...", event.eventCreatedAt)
            when (event.eventType) {
                Event.Type.ORDER_CREATED -> {
                    val order: Order = event.data!!
                    LOG.info("Check if {} items (productId: {}) are available in warehouse : {}", order.amount, order.orderId)
                    warehouseService.checkAvailability(order)!!.block()
                }
                Event.Type.CREDIT_RESERVED -> {
                    val order: Order = event.data!!
                    LOG.info("Decrease the products in the warehouse by the quantity of each product in the order with ID: {}", order.orderId)
                    warehouseService.decrementQuantity(order)!!.block()
                }
                Event.Type.ROLLBACK_QUANTITY -> {
                    val productId: Int = event.key!!
//                    LOG.info("Delete recommendations with ProductID: {}", productId)
//                    productService.deleteProduct(productId).block()
                }
                else -> {
                    val errorMessage = "Incorrect event type: " + event.eventType
                        .toString() + ", expected a CREATE or DELETE event"
                    LOG.warn(errorMessage)
                    throw EventProcessingException(errorMessage)
                }
            }
            LOG.info("Message processing done!")
        }
    }

    companion object {
        val LOG = LoggerFactory.getLogger(MessageProcessorConfig::class.java)
    }

    init {
        this.warehouseService = warehouseService
    }
}