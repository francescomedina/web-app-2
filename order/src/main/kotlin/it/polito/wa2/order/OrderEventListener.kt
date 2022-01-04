package it.polito.wa2.order

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.polito.wa2.order.domain.ProductEntity
import it.polito.wa2.order.dto.OrderDTO
import it.polito.wa2.order.dto.ProductDTO
import it.polito.wa2.order.dto.toOrderDTO
import it.polito.wa2.order.services.OrderServiceImpl
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.ProducerRecord
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Component
import java.math.BigDecimal

data class OrderEntity(
    @JsonProperty("id")
    var id: ObjectId,
    @JsonProperty("status")
    var status: String?,
    @JsonProperty("buyer")
    var buyer: String,
    @JsonProperty("products")
    var products: List<ProductEntity> = emptyList(),
)

data class Response(
    @JsonProperty("order")
    var order: OrderEntity,
    @JsonProperty("response")
    var response: String
)

@Component
class OrderEventListener(
    val orderService: OrderServiceImpl
) {
    private val logger = LoggerFactory.getLogger(OrderEventListener::class.java)

    @KafkaListener(topics = ["warehouse.topic"])
    fun orderConfirmed(
        @Payload payload: String,
        @Header("type") type: String?
    ) {
        val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
        val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
        logger.info("ORDER Received: ${order}")
        if(type != null && type == "QUANTITY_DECREMENTED"){
            val orderDTO = OrderDTO(
                order.id,
                "ISSUED",
                order.buyer,
                order.products.map { ProductEntity(it.id,it.quantity,it.price).toOrderDTO() }.toList()
            )
            runBlocking {
                orderService.updateOrder(
                    null,
                    orderDTO.id.toString(),
                    orderDTO,
                    null,
                    true
                )
            }
        }else if(type == "QUANTITY_UNAVAILABLE"){
            val orderDTO = OrderDTO(
                order.id,
                "FAILED-QUANTITY_UNAVAILABLE",
                order.buyer,
                order.products.map { ProductEntity(it.id,it.quantity,it.price).toOrderDTO() }.toList()
            )
            runBlocking {
                orderService.updateOrder(
                    null,
                    orderDTO.id.toString(),
                    orderDTO,
                    null,
                    true
                )
            }
        }
    }

    @KafkaListener(topics = ["wallet.topic"])
    fun orderCanceled(
        @Payload payload: String,
        @Header("type") type: String
    ) {
        if(type == "REFUND_TRANSACTION_SUCCESS"){
            logger.info("ORDER Received 54: $payload")
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java) // uso GenericMessage perché nel json c'è anche lo schema: {}
            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
            logger.info("ORDER Received: $order")
            val orderDTO = OrderDTO(
                order.id,
                "CANCELED",
                order.buyer,
                order.products.map { ProductEntity(it.id,it.quantity,it.price).toOrderDTO() }.toList()
            )
            logger.info("ORDER Received 2: $orderDTO")
            runBlocking {
                orderService.updateOrder(
                    null,
                    orderDTO.id.toString(),
                    orderDTO,
                    null,
                    true
                )
            }
        }else if(type == "CREDIT_UNAVAILABLE"){
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java) // uso GenericMessage perché nel json c'è anche lo schema: {}
            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
            logger.info("ORDER Received: $order")
            val orderDTO = OrderDTO(
                order.id,
                "FAILED-CREDIT_UNAVAILABLE",
                order.buyer,
                order.products.map { ProductEntity(it.id,it.quantity,it.price).toOrderDTO() }.toList()
            )
            logger.info("ORDER Received 2: $orderDTO")
            runBlocking {
                orderService.updateOrder(
                    null,
                    orderDTO.id.toString(),
                    orderDTO,
                    null,
                    true
                )
            }
        }
    }
}
