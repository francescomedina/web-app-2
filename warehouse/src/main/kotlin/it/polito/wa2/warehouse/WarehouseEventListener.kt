package it.polito.wa2.warehouse

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import it.polito.wa2.warehouse.outbox.OutboxEventPublisher
import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
import it.polito.wa2.warehouse.repository.WarehouseRepository
import it.polito.wa2.warehouse.services.ProductServiceImpl
import it.polito.wa2.warehouse.services.WarehouseServiceImpl
import it.polito.wa2.warehouse.utils.ObjectIdTypeAdapter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingleOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
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
import reactor.core.publisher.Mono
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
@Transactional
class WarehouseEventListener constructor(
    val errorProducer: ErrorProducer,
    val warehouseRepository: WarehouseRepository,
    val warehouseServiceImpl: WarehouseServiceImpl,
    val productServiceImpl: ProductServiceImpl,
    val productAvailabilityRepository: ProductAvailabilityRepository,
    val transactionalOperator: TransactionalOperator,
    val eventPublisher: OutboxEventPublisher,
    @Value("\${topics.out}")
    private val topicTarget: String,
    @Value("\${topics.out-error}")
    private val errorTopicTarget: String,
    val kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(WarehouseEventListener::class.java)

    fun updateQuantity(){
        
    }

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(
        @Payload payload: String,
        @Header("type") type: String?
    ) {
        if(type == "ORDER_CREATED"){
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
            logger.info("Received: $order")
            order.products.map {
                runBlocking {
                    val warehouses = productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)?.awaitSingleOrNull()
                    logger.info("WAREHOUSE ESISTEEEEEEEEEEEEEEEEE: $warehouses")
                    if(warehouses == null){
                        kafkaTemplate.send(ProducerRecord("order.topic", order.id.toString(), gson.toJson(Result(order,"QUANTITY_UNAVAILABLE"))))
                    }
                }
            }
            kafkaTemplate.send(ProducerRecord("warehouse.topic", order.id.toString(), gson.toJson(Result(order,"QUANTITY_AVAILABLE"))))
        }
        else if(type == "ORDER_CANCELED"){
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
            logger.info("Received: $order")
            kafkaTemplate.send(ProducerRecord("warehouse.topic", order.id.toString(), gson.toJson(Result(order,"QUANTITY_INCREMENTED"))))
        }
    }

    @Transactional
    suspend fun saveOrderProducts(order: OrderEntity) {
        logger.info("ENTRATOOO CON $order")
/*        return Flux.fromIterable(order.products)
            .doOnNext {
                logger.info("ENTRATO NEL DO ON NEXT $it")
                runBlocking {
                    val productAvailabilityEntity = productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)?.awaitSingleOrNull()
                    logger.info("DO ON NEXT $productAvailabilityEntity")
                    Assert.isTrue(productAvailabilityEntity!=null, "WAREHOUSE-EXCEPTION Product is no more available, sending rollback wallet action")
                    if(productAvailabilityEntity!=null){
                        productAvailabilityEntity.quantity -= it.quantity
                        runBlocking {
                            productAvailabilityRepository.save(productAvailabilityEntity).awaitSingle()
                        }
                    }
                }
            }
            .doOnComplete {
                logger.info("FLUX completato")
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
                logger.info("ERRORE nel FLUX")
                runBlocking {
                    eventPublisher.publish(
                        "wallet.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "QUANTITY_UNAVAILABLE"
                    )
                }
            }*/
        return try {
            order.products.asFlow().collect {
                val productAvailabilityEntity = productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)?.awaitSingleOrNull()
                logger.info("DO ON NEXT $productAvailabilityEntity")
                Assert.isTrue(productAvailabilityEntity!=null, "WAREHOUSE-EXCEPTION Product is no more available, sending rollback wallet action")
                if(productAvailabilityEntity!=null){
                    productAvailabilityEntity.quantity -= it.quantity
                    productAvailabilityRepository.save(productAvailabilityEntity).awaitSingle()
                }
            }
            logger.info("FLUX completato")
            eventPublisher.publish(
                "warehouse.topic",
                order.id.toString(),
                gson.toJson(order),
                "QUANTITY_DECREMENTED"
            )
        } catch (e: Throwable) {
            logger.info("ERRORE nel FLUX $e")
            eventPublisher.publish(
                "wallet.topic",
                order.id.toString(),
                gson.toJson(order),
                "QUANTITY_UNAVAILABLE"
            )
        }
/*        return order.products.asFlow()
            .onEach {
                val productAvailabilityEntity = productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)?.awaitSingleOrNull()
                logger.info("DO ON NEXT $productAvailabilityEntity")
                Assert.isTrue(productAvailabilityEntity!=null, "WAREHOUSE-EXCEPTION Product is no more available, sending rollback wallet action")
                if(productAvailabilityEntity!=null){
                    productAvailabilityEntity.quantity -= it.quantity
                    productAvailabilityRepository.save(productAvailabilityEntity).awaitSingle()
                }
            }
            .catch {
                logger.info("ERRORE nel FLUX")
                eventPublisher.publish(
                    "wallet.topic",
                    order.id.toString(),
                    gson.toJson(order),
                    "QUANTITY_UNAVAILABLE"
                )
            }
            .collect {  }*/
    }


    @KafkaListener(topics = ["wallet.topic"])
    suspend fun decrementQuantity(
        @Payload payload: String,
        @Header("type") type: String
    ) {
        logger.info("WAREHOUSE_2 Received: ${type}")
        logger.info("WAREHOUSE_2 Received: ${payload}")
        if(type == "TRANSACTION_SUCCESS"){
            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
            val res = gson.fromJson(genericMessage.payload.toString(),Result::class.java)
            saveOrderProducts(res.order)
        }
    }
}
