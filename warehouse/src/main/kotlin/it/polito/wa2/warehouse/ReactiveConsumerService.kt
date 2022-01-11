package it.polito.wa2.warehouse

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import it.polito.wa2.warehouse.outbox.OutboxEventPublisher
import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.*

data class ProductEntity(
    @JsonProperty("id")
    var id: ObjectId,
    @JsonProperty("quantity")
    var quantity: Int,
    @JsonProperty("price")
    var price: BigDecimal
)

data class DeliveryEntity(
    @JsonProperty("shippingAddress")
    var shippingAddress: String?,
    @JsonProperty("warehouseId")
    var warehouseId: ObjectId,
    @JsonProperty("products")
    var products: List<ProductEntity> = emptyList(),
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
    @JsonProperty("delivery")
    var delivery: List<DeliveryEntity>? = emptyList(),
)


@Service
class ReactiveConsumerService(
    val reactiveKafkaConsumerTemplate: ReactiveKafkaConsumerTemplate<String, String>,
    val reactiveProducerService: ReactiveProducerService,
    val productAvailabilityRepository: ProductAvailabilityRepository,
    val eventPublisher: OutboxEventPublisher,
) : CommandLineRunner {

    var log = LoggerFactory.getLogger(ReactiveConsumerService::class.java)

    @Transactional
    fun checkProductAvailability(order: OrderEntity): Flux<ProductEntity> {
        return Mono.just(order.products)
            .flatMapMany { products ->
                Flux.fromIterable(products)
                    .doOnNext {
                        productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
                            .doOnNext { p -> Assert.isTrue(p!=null,"Product no more available") }
                            .subscribe()
                    }
                    .doOnError {
                        reactiveProducerService.send(
                            ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
                        )
                        throw RuntimeException(it)
                    }
            }
            .doOnComplete {
                reactiveProducerService.send(
                    ProducerRecord(
                        "warehouse.topic",
                        null,
                        order.id.toString(),
                        gson.toJson(order),
                        listOf(RecordHeader("type", "QUANTITY_AVAILABLE".toByteArray()))
                    )
                )
            }
    }

    private fun warehouseConsumer(): Flux<ConsumerRecord<String, String>> {
        return reactiveKafkaConsumerTemplate
            .receiveAutoAck()
            .doOnNext { consumerRecord: ConsumerRecord<String, String> ->
                log.info(
                    "received key={}, value={} from topic={}, offset={}, headers={}",
                    consumerRecord.key(),
                    consumerRecord.value(),
                    consumerRecord.topic(),
                    consumerRecord.offset(),
                    consumerRecord.headers()
                )
            }
            .doOnNext {
                val type = String(it.headers().reduce { _, header -> if(header.key() == "type") header else null}?.value() as ByteArray)
                val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
                when (type){
                    "ORDER_CREATED" -> checkProductAvailability(order).subscribe()
                    "ORDER_CANCELED" -> incrementQuantity(order).subscribe()
                    "TRANSACTION_SUCCESS" -> decrementQuantity(order).subscribe()
                    "REFUND_TRANSACTION_ERROR" -> incrementQuantity(order, true).subscribe()
                }
            }
            .doOnError { throwable: Throwable ->
                log.error("something bad happened while consuming : {}", throwable.message)
            }
    }

    @Transactional
    fun decrementQuantity(order: OrderEntity): Flux<ProductAvailabilityEntity> {
        val deliveryMap = hashMapOf<ObjectId, MutableList<ProductEntity>>()
        return Flux.fromIterable(order.products)
                .flatMap {
                    productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
                        .doOnNext { p ->
                            Assert.isTrue(p!=null, "Product is no more available")
                            p!!.quantity -= it.quantity
                            if(deliveryMap[p!!.warehouseId].isNullOrEmpty()){
                                deliveryMap[p.warehouseId] = mutableListOf(it)
                            }else{
                                deliveryMap[p.warehouseId]?.add(it)
                            }
                        }
                }
                .flatMap { productAvailabilityRepository.save(it!!) }
                .doOnError { throw RuntimeException("Error on productAvailability") }
                .doOnComplete { // Si applica alla fine di tutti i flussi, in questo caso dopo aver iterato tutti i prodotti
                    val tmp = mutableListOf<DeliveryEntity>()
                    deliveryMap.forEach {
                        tmp.add(DeliveryEntity(
                            "Via Alessandro Volta 123",
                            it.key,
                            it.value.toList()
                        ))
                    }
                    order.delivery = tmp.toList()
                    eventPublisher.publish(
                        "warehouse.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "QUANTITY_DECREMENTED"
                    ).subscribe{
                        log.info("successfully consumed DECREMENT QUANTITY {} ", it)
                    }
                }
                .doOnError {
                    eventPublisher.publish(
                        "wallet.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "QUANTITY_UNAVAILABLE"
                    ).subscribe {
                        throw RuntimeException("Error during updating products")
                    }
                }
    }

    @Transactional
    fun incrementQuantity(order: OrderEntity, dueToError: Boolean = false): Flux<DeliveryEntity> {
        return Flux.fromIterable(order.delivery!!)
            .doOnNext {
                Flux.fromIterable(it.products)
                    .doOnNext { p ->
                        productAvailabilityRepository.findOneByWarehouseIdAndProductId(it!!.warehouseId,p.id)
                            .doOnNext { pa ->
                                pa!!.quantity += p.quantity
                                productAvailabilityRepository.save(pa!!).subscribe()
                            }.subscribe()
                    }
                    .subscribe()
            }
            .doOnError { throw RuntimeException("Error on productAvailability") }
            .doOnComplete {
                if(!dueToError){
                    eventPublisher.publish(
                        "warehouse.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "WAREHOUSE_PRODUCTS_RETURNED"
                    ).subscribe{
                        log.info("successfully consumed INCREMENT QUANTITY {} ", it)
                    }
                }
            }
            .doOnError {
                eventPublisher.publish(
                    "warehouse.topic",
                    order.id.toString(),
                    gson.toJson(order),
                    "WAREHOUSE_PRODUCTS_RETURNING_ERROR"
                ).subscribe()
            }
    }

    override fun run(vararg args: String) {
        warehouseConsumer().subscribe()
    }
}