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
import it.polito.wa2.order.utils.ObjectIdTypeAdapter
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

//data class ProductEntity(
//    @JsonProperty("id")
//    var id: ObjectId,
//    @JsonProperty("quantity")
//    var quantity: Int,
//    @JsonProperty("price")
//    var price: BigDecimal
//)

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
class OrderEventListener@Autowired constructor(
    orderService: OrderServiceImpl
) {

    private val orderService: OrderServiceImpl
    private val logger = LoggerFactory.getLogger(OrderEventListener::class.java)
    init {
        this.orderService = orderService
    }

//    @KafkaListener(topics = ["\${topics.in}"])
//    fun listen(message: Message<String>) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")
//    }

    @KafkaListener(topics = ["warehouse.topic"])
    fun orderConfirmed(
        @Payload payload: String
    ) {
        val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
        val response = gson.fromJson(payload, Response::class.java)
        logger.info("ORDER Received: ${response.response}")
        if(response.response == "QUANTITY_DECREMENTED"){
            val orderDTO = OrderDTO(
                response.order.id,
                "ISSUED",
                response.order.buyer,
                response.order.products.map { ProductEntity(it.id,it.quantity,it.price).toOrderDTO() }.toList()
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
            val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
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
        }
    }
}
