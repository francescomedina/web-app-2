package it.polito.wa2.warehouse

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.warehouse.persistence.WarehouseRepository
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
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.*

data class Result(
    val order: OrderDTO,
    val response: String
)

data class ProductDTO(
    @Id
    val id: ObjectId,
    val amount: BigDecimal,
    val price: BigDecimal
)

data class OrderDTO(
    val buyer: String,
    val products: List<ProductDTO>
)


@Component
class WarehouseEventListener @Autowired constructor(
    errorProducer: ErrorProducer,
    warehouseRepository: WarehouseRepository,
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

    init {
        this.errorProducer = errorProducer
        this.kafkaTemplate = kafkaTemplate
        this.warehouseRepository = warehouseRepository
    }

    @KafkaListener(topics = ["\${topics.in}"])
    suspend fun listen(
        @Payload payload: String,
        @Header("aggregate_id") aggregateId: String,
        @Header("message_id") messageId: String,
        @Header("type") type: String
    ) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")

//        if(false){
//            exampleService.addExample(topicTarget,ExampleEntity("Quantity Available"))
//        }else{
//            errorProducer.produce(errorTopicTarget,"123", message.payload)
//        }
        val mapper = ObjectMapper()
        val order = mapper.readValue(payload, OrderDTO::class.java)
        order.products.map {
            val warehouses = warehouseRepository.findAllByProductIdAndAmountGreaterThanEqual(it.id,it.amount)
                ?.awaitSingleOrNull()
            if(warehouses == null){
                kafkaTemplate.send(ProducerRecord("order.topic", aggregateId, Result(order,"QUANTITY UNAVAILABLE").toString()))
            }
        }
        kafkaTemplate.send(ProducerRecord("warehouse.topic", aggregateId, Result(order,"QUANTITY AVAILABLE").toString()))
    }

    @KafkaListener(topics = ["wallet.topic"])
    fun decrementQuantity(
        @Payload payload: String,
        @Header("aggregate_id") aggregateId: String,
        @Header("message_id") messageId: String,
        @Header("type") type: String
    ) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")

//        if(false){
//            exampleService.addExample(topicTarget,ExampleEntity("Quantity Available"))
//        }else{
//            errorProducer.produce(errorTopicTarget,"123", message.payload)
//        }
        val key = aggregateId + '_' + messageId + '_' + type
        kafkaTemplate.send(ProducerRecord("warehouse.topic", key, payload))
    }

//    @KafkaListener(topics = ["\${topics.in-error}"])
//    fun error(message: Message<String>) {
////        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
////        logger.info("Received: ${message.payload}")
//
//        if(false){
//            exampleService.addExample(topicTarget,ExampleEntity("Quantity Available"))
//        }else{ // Quantità non disponibile
//            errorProducer.produce("order.topic", "123",message.payload)
//        }
//    }
}
