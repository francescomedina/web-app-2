package it.polito.wa2.warehouse

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import it.polito.wa2.warehouse.outbox.OutboxEventPublisher
import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
import it.polito.wa2.warehouse.repository.WarehouseRepository
import it.polito.wa2.warehouse.services.ProductServiceImpl
import it.polito.wa2.warehouse.services.WarehouseServiceImpl
import it.polito.wa2.warehouse.utils.ObjectIdTypeAdapter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
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
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.util.Assert
import reactor.core.publisher.Flux
import reactor.core.publisher.toFlux
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
    warehouseServiceImpl: WarehouseServiceImpl,
    productServiceImpl: ProductServiceImpl,
    productAvailabilityRepository: ProductAvailabilityRepository,
    transactionalOperator: TransactionalOperator,
    eventPublisher: OutboxEventPublisher,
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
    private val warehouseServiceImpl: WarehouseServiceImpl
    private val productServiceImpl: ProductServiceImpl
    private val productAvailabilityRepository: ProductAvailabilityRepository
    private val transactionalOperator: TransactionalOperator
    private val eventPublisher: OutboxEventPublisher

    init {
        this.errorProducer = errorProducer
        this.kafkaTemplate = kafkaTemplate
        this.warehouseRepository = warehouseRepository
        this.warehouseServiceImpl = warehouseServiceImpl
        this.productServiceImpl = productServiceImpl
        this.productAvailabilityRepository = productAvailabilityRepository
        this.transactionalOperator = transactionalOperator
        this.eventPublisher = eventPublisher
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
            order.products.map {
                val warehouses = productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
                if(warehouses == null){
                    kafkaTemplate.send(ProducerRecord("order.topic", messageId, gson.toJson(Result(order,"QUANTITY_UNAVAILABLE"))))
                }
            }
            kafkaTemplate.send(ProducerRecord("warehouse.topic", messageId, gson.toJson(Result(order,"QUANTITY_AVAILABLE"))))
        }
        else if(type == "ORDER_CANCELED"){
            val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
            logger.info("Received: $order")
            kafkaTemplate.send(ProducerRecord("warehouse.topic", messageId, gson.toJson(Result(order,"QUANTITY_INCREMENTED"))))
        }
    }

    @Transactional
    fun saveOrderProducts(order: OrderEntity): Flux<ProductEntity> {
        val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
        return Flux.just(order.products)
            .flatMapIterable { it }
            .doOnNext {
                val productAvailabilityEntity = productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
                Assert.isTrue(productAvailabilityEntity!=null, "No warehouse found for this product")
                if(productAvailabilityEntity!=null){
                    Assert.isTrue(productAvailabilityEntity.quantity - it.quantity >= 0, "Quantity unavailable")
                    productAvailabilityEntity.quantity -= it.quantity
                    runBlocking {
                        productAvailabilityRepository.save(productAvailabilityEntity).awaitSingle()
                    }
                }
            }
            .doOnComplete {
                runBlocking {
                    eventPublisher.publish(
                        "warehouse.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "QUANTITY_DECREMENTED"
                    )
                }
            }
            .doOnError {
                runBlocking {
                    eventPublisher.publish(
                        "wallet.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "QUANTITY_UNAVAILABLE"
                    )
                }
            }
    }

    @Transactional
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
            saveOrderProducts(res.order)
        }
    }
}
