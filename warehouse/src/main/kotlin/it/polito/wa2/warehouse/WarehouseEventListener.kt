//package it.polito.wa2.warehouse
//
//import com.fasterxml.jackson.annotation.JsonProperty
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.google.gson.Gson
//import com.google.gson.GsonBuilder
//import it.polito.wa2.api.exceptions.AppRuntimeException
//import it.polito.wa2.util.gson.GsonUtils.Companion.gson
//import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
//import it.polito.wa2.warehouse.outbox.OutboxEventPublisher
//import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
//import it.polito.wa2.warehouse.repository.WarehouseRepository
//import it.polito.wa2.warehouse.services.ProductServiceImpl
//import it.polito.wa2.warehouse.services.WarehouseServiceImpl
//import it.polito.wa2.warehouse.utils.ObjectIdTypeAdapter
//import kotlinx.coroutines.flow.*
//import kotlinx.coroutines.reactive.awaitFirst
//import kotlinx.coroutines.reactive.awaitSingleOrNull
//import kotlinx.coroutines.reactor.awaitSingle
//import kotlinx.coroutines.reactor.awaitSingleOrNull
//import kotlinx.coroutines.runBlocking
//import org.apache.kafka.clients.producer.ProducerRecord
//import org.bson.types.ObjectId
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.data.annotation.Id
//import org.springframework.http.HttpStatus
//import org.springframework.kafka.annotation.KafkaListener
//import org.springframework.kafka.core.KafkaTemplate
//import org.springframework.messaging.Message
//import org.springframework.messaging.handler.annotation.Header
//import org.springframework.messaging.handler.annotation.Payload
//import org.springframework.messaging.support.GenericMessage
//import org.springframework.stereotype.Component
//import org.springframework.transaction.annotation.Transactional
//import org.springframework.transaction.reactive.TransactionalOperator
//import org.springframework.util.Assert
//import reactor.core.publisher.Flux
//import reactor.core.publisher.Mono
//import reactor.core.publisher.toFlux
//import java.math.BigDecimal
//import java.util.*
//import javax.validation.constraints.NotBlank
//import javax.xml.validation.Schema
//
//data class Result(
//    val order: OrderEntity,
//    val response: String
//)
//
//data class ProductEntity(
//    @JsonProperty("id")
//    var id: ObjectId,
//    @JsonProperty("quantity")
//    var quantity: Int,
//    @JsonProperty("price")
//    var price: BigDecimal
//)
//
//data class OrderEntity(
//    @JsonProperty("id")
//    var id: ObjectId,
//    @JsonProperty("status")
//    var status: String? = null,
//    @JsonProperty("buyer")
//    var buyer: String? = null,
//    @JsonProperty("products")
//    var products: List<ProductEntity> = emptyList(),
//)
//
//
//@Component
//@Transactional
//class WarehouseEventListener constructor(
//    val productAvailabilityRepository: ProductAvailabilityRepository,
//    val eventPublisher: OutboxEventPublisher,
//    val kafkaTemplate: KafkaTemplate<String, String>
//) {
//    private val logger = LoggerFactory.getLogger(WarehouseEventListener::class.java)
//
//    @KafkaListener(topics = ["\${topics.in}"])
//    fun listen(
//        @Payload payload: String,
//        @Header("type") type: String?
//    ) {
//        if(type == "ORDER_CREATED"){
//            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
//            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
//            logger.info("Received: $order")
//            Flux.fromIterable(order.products)
//                .doOnNext {
//                    productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
//                        .doOnNext { p -> Assert.isTrue(p!=null,"Product no more available") }
//                        .subscribe()
//                }
//                .doOnError {
//                    kafkaTemplate.send(ProducerRecord("warehouse.topic", order.id.toString(), gson.toJson(Result(order,"QUANTITY_AVAILABLE"))))
//                }
//                .doOnNext {
//                    kafkaTemplate.send(ProducerRecord("order.topic", order.id.toString(), gson.toJson(Result(order,"QUANTITY_UNAVAILABLE"))))
//                }
//                .subscribe()
//        }
//        else if(type == "ORDER_CANCELED"){
//            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
//            val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
//            logger.info("Received: $order")
//            kafkaTemplate.send(ProducerRecord("warehouse.topic", order.id.toString(), gson.toJson(Result(order,"QUANTITY_INCREMENTED"))))
//        }
//    }
//
//    /**
//     * TRANSACTIONAL: changes are committed if no exceptions are generated, rollback otherwise
//     */
//    fun updateQuantity(order: OrderEntity): Flux<ProductAvailabilityEntity> {
//        return Flux.fromIterable(order.products)
//            .flatMap {
//                productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
//                    .doOnNext { p ->
//                        Assert.isTrue(p!=null, "Product is no more available")
//                        p!!.quantity -= it.quantity
//                    }
//            }
//            .flatMap { productAvailabilityRepository.save(it!!) }
//            .doOnNext {
//                eventPublisher.publish(
//                    "warehouse.topic",
//                    order.id.toString(),
//                    gson.toJson(order),
//                    "QUANTITY_DECREMENTED"
//                ).subscribe()
//            }
//            .doOnError {
//                eventPublisher.publish(
//                    "wallet.topic",
//                    order.id.toString(),
//                    gson.toJson(order),
//                    "QUANTITY_UNAVAILABLE"
//                ).subscribe()
//            }
//    }
//
//    @KafkaListener(topics = ["wallet.topic"])
//    suspend fun decrementQuantity(
//        @Payload payload: String,
//        @Header("type") type: String
//    ) {
//        logger.info("WAREHOUSE_2 Received: ${type}")
//        logger.info("WAREHOUSE_2 Received: ${payload}")
//        if(type == "TRANSACTION_SUCCESS"){
//            val genericMessage = gson.fromJson(payload, GenericMessage::class.java)
//            val res = gson.fromJson(genericMessage.payload.toString(),Result::class.java)
//            updateQuantity(res.order)
//        }
//    }
//}
