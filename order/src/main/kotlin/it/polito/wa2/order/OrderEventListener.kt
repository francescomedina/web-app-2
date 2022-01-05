package it.polito.wa2.order

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.order.domain.ProductEntity
import it.polito.wa2.order.dto.OrderDTO
import it.polito.wa2.order.dto.toOrderDTO
import it.polito.wa2.order.services.OrderServiceImpl
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Component

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
        val status = when (type) {
            "QUANTITY_DECREMENTED" -> "ISSUED"
            "QUANTITY_UNAVAILABLE" -> "FAILED-QUANTITY_UNAVAILABLE"
            else -> ""
        }
        if(status=="") return
        orderService.updateOrder(
            null,
            order.id.toString(),
            OrderDTO(
                order.id,
                status,
                order.buyer,
                order.products.map { ProductEntity(it.id,it.quantity,it.price).toOrderDTO() }.toList()
            ),
            null,
            true
        ).subscribe()
    }

    @KafkaListener(topics = ["wallet.topic"])
    fun orderCanceled(
        @Payload payload: String,
        @Header("type") type: String
    ) {
        val status = when (type) {
            "REFUND_TRANSACTION_SUCCESS" -> "CANCELED"
            "CREDIT_UNAVAILABLE" -> "FAILED-CREDIT_UNAVAILABLE"
            "TRANSACTION_ERROR" -> "FAILED-TRANSACTION_ERROR"
            else -> ""
        }
        if(status=="") return
        val genericMessage = gson.fromJson(payload, GenericMessage::class.java) // uso GenericMessage perché nel json c'è anche lo schema: {}
        val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
        orderService.updateOrder(
            null,
            order.id.toString(),
            OrderDTO(
                order.id,
                status,
                order.buyer,
                order.products.map { ProductEntity(it.id,it.quantity,it.price).toOrderDTO() }.toList()
            ),
            null,
            true
        ).subscribe()
    }
}
