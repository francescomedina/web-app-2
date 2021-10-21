package it.polito.wa2.warehouse.services

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
    fun messageProcessor(): Consumer<Event<Int?, Warehouse?>> {
        return Consumer<Event<Int?, Warehouse?>> { event: Event<Int?, Warehouse?> ->
            LOG.info("Process message created at {}...", event.eventCreatedAt)
            when (event.eventType) {
                Event.Type.CREATE -> {
                    val warehouse: Warehouse = event.data!!
                    LOG.info("Create warehouse with ID: {}", warehouse.orderId)
                    warehouseService.createWarehouse(warehouse)!!.block()
                }
                Event.Type.DELETE -> {
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