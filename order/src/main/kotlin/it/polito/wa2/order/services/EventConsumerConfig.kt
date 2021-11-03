package it.polito.wa2.order.services

import it.polito.wa2.api.core.order.OrderService
import it.polito.wa2.api.event.WarehouseEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.function.Consumer

@EnableAutoConfiguration
@Component
class EventConsumerConfig @Autowired constructor(handler: OrderStatusUpdateHandler?){
    private val handler: OrderStatusUpdateHandler?

    @Bean
    fun warehouseEventConsumer(): Consumer<WarehouseEvent> {
        //listen payment-event-topic
        //will check payment status
        //if payment status completed -> complete the order
        //if payment status failed -> cancel the order
        return Consumer<WarehouseEvent> { warehouse ->
            handler!!.updateOrder(warehouse.key) {
                w -> w!!.status
            }
        }
    }

    init {
        this.handler = handler
    }
}