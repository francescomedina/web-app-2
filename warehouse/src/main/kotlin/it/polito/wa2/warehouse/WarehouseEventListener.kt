package it.polito.wa2.warehouse

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
import it.polito.wa2.warehouse.repository.WarehouseRepository
import it.polito.wa2.warehouse.utils.ObjectIdTypeAdapter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingleOrNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.annotation.Id
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*
import javax.validation.constraints.NotBlank
import javax.xml.validation.Schema

data class Result(
    val order: OrderEntity,
    val response: String
)

data class ProductEntity(
    @JsonProperty("id")
    var id: ObjectId,
    @JsonProperty("quantity")
    var quantity: Int,
    @JsonProperty("price")
    var price: BigDecimal
)

data class OrderEntity(
    @JsonProperty("id")
    var id: ObjectId,
    @JsonProperty("status")
    var status: String? = null,
    @JsonProperty("buyer")
    var buyer: String? = null,
    @JsonProperty("products")
    var products: List<ProductEntity> = emptyList(),
)


@Component
class WarehouseEventListener @Autowired constructor(
    errorProducer: ErrorProducer,
    warehouseRepository: WarehouseRepository,
    productAvailabilityRepository: ProductAvailabilityRepository,
    @Value("\${topics.out}")
    private val topicTarget: String,
    @Value("\${topics.out-error}")
    private val errorTopicTarget: String,
    kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(WarehouseEventListener::class.java)
    private val errorProducer: ErrorProducer
    private val kafkaTemplate: KafkaTemplate<String, String>
    private val warehouseRepository: WarehouseRepository
    private val productAvailabilityRepository: ProductAvailabilityRepository

    init {
        this.errorProducer = errorProducer
        this.kafkaTemplate = kafkaTemplate
        this.warehouseRepository = warehouseRepository
        this.productAvailabilityRepository = productAvailabilityRepository
    }

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(
        @Payload payload: String,
        @Header("message_id") messageId: String,
        @Header("type") type: String
    ) {
        if(type == "ORDER_CREATED"){
            val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
            logger.info("Received: $order")
            kafkaTemplate.send(ProducerRecord("warehouse.topic", messageId, gson.toJson(Result(order,"QUANTITY_AVAILABLE"))))
        }
        else if(type == "ORDER_CANCELED"){
            val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
            logger.info("Received: $order")
            kafkaTemplate.send(ProducerRecord("warehouse.topic", messageId, gson.toJson(Result(order,"QUANTITY_INCREMENTED"))))
        }
//        order.products.map {
//            val warehouses = productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
//            if(warehouses == null){
//                kafkaTemplate.send(ProducerRecord("order.topic", messageId, Result(order,"QUANTITY UNAVAILABLE").toString()))
//            }
//        }
//        kafkaTemplate.send(ProducerRecord("warehouse.topic", aggregateId, Result(order,"QUANTITY AVAILABLE").toString()))
    }

    @KafkaListener(topics = ["wallet.topic"])
    fun decrementQuantity(
        @Payload payload: String,
        @Header("type") type: String
    ) {
        logger.info("WAREHOUSE_2 Received: ${type}")
        logger.info("WAREHOUSE_2 Received: ${payload}")
        if(type == "TRANSACTION_SUCCESS"){
            val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
            val res = gson.fromJson(genericMessage.payload.toString(),Result::class.java)
            kafkaTemplate.send(ProducerRecord("warehouse.topic", res.order.id.toString(), gson.toJson(Result(res.order,"QUANTITY_DECREMENTED"))))
        }
    }
}
