package it.polito.wa2.order

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.order.dto.OrderDTO
import it.polito.wa2.order.services.OrderServiceImpl
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class OrderEventListener@Autowired constructor(
    orderService: OrderServiceImpl
) {

    private val orderService: OrderServiceImpl
    private val logger = LoggerFactory.getLogger(OrderEventListener::class.java)
    init {
        this.orderService = orderService
    }

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(message: Message<String>) {
        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
        logger.info("Received: ${message.payload}")
    }

    @KafkaListener(topics = ["warehouse.topic"])
    suspend fun orderConfirmed(
        @Payload payload: String,
        @Header("aggregate_id") aggregateId: String,
        @Header("message_id") messageId: String,
        @Header("type") type: String
    ) {

        if(type === "QUANTITY_DECREMENTED"){
            val mapper = ObjectMapper()
            val order: OrderDTO = mapper.readValue(payload, OrderDTO::class.java)
            order.status = "ISSUED"
            orderService.updateOrder(
                null,
                aggregateId,
                order,
                null,
                true
            )
        }
    }

    @KafkaListener(topics = ["warehouse.topic"])
    suspend fun orderCanceled(
        @Payload payload: String,
        @Header("aggregate_id") aggregateId: String,
        @Header("message_id") messageId: String,
        @Header("type") type: String
    ) {

        if(type === "QUANTITY_INCREMENTED_ORDER_CANCELED"){
            val mapper = ObjectMapper()
            val order: OrderDTO = mapper.readValue(payload, OrderDTO::class.java)
            order.status = "CANCELED"
            orderService.updateOrder(
                null,
                aggregateId,
                order,
                null,
                true
            )
        }
    }
}
