package it.polito.wa2.warehouse

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import it.polito.wa2.warehouse.outbox.OutboxEventPublisher
import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
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
                            ProducerRecord("asd.topic", order.id.toString(), gson.toJson(Result(order,"QUANTITY_UNAVAILABLE")))
                        )
                        throw RuntimeException(it)
                    }
            }
            .doOnComplete {
                reactiveProducerService.send(
                    ProducerRecord(
                        "warehouse.topic",
                        order.id.toString(),
                        gson.toJson(Result(order,"QUANTITY_AVAILABLE"))
                    )
                )
            }
    }

    private fun warehouseConsumer(): Flux<ConsumerRecord<String, String>> {
        return reactiveKafkaConsumerTemplate
            .receiveAutoAck() // .delayElements(Duration.ofSeconds(2L)) // BACKPRESSURE
            .doOnNext { consumerRecord: ConsumerRecord<String, String> ->
                log.info(
                    "received key={}, value={} from topic={}, offset={}",
                    consumerRecord.key(),
                    consumerRecord.value(),
                    consumerRecord.topic(),
                    consumerRecord.offset()
                )
            }
            .doOnNext {
                val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                if(it.headers().any { h -> h.key().toString() == "type" && h.value().toString() == "ORDER_CREATED" }){
                    val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
                    checkProductAvailability(order).subscribe()
                }
                else if(it.headers().any { h -> h.key().toString() == "type" && h.value().toString() == "ORDER_CANCELED" }){
                    val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
                    reactiveProducerService.send(ProducerRecord("warehouse.topic", order.id.toString(), gson.toJson(Result(order,"QUANTITY_INCREMENTED"))))
                }else if(it.headers().any { h -> h.key().toString() == "type" && h.value().toString() == "TRANSACTION_SUCCESS" }){
                    val res = gson.fromJson(genericMessage.payload.toString(), Result::class.java)
                    updateQuantity(res.order)
                }
                log.info("successfully consumed {}={}", GenericMessage::class.java.simpleName, genericMessage)
            }
            .doOnError { throwable: Throwable ->
                log.error("something bad happened while consuming : {}", throwable.message)
            }
    }

    @Transactional
    fun updateQuantity(order: OrderEntity): Flux<ProductAvailabilityEntity> {
        return Flux.fromIterable(order.products)
            .flatMap {
                productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
                    .doOnNext { p ->
                        Assert.isTrue(p!=null, "Product is no more available")
                        p!!.quantity -= it.quantity
                    }
            }
            .flatMap { productAvailabilityRepository.save(it!!) }
            .doOnNext {
                eventPublisher.publish(
                    "warehouse.topic",
                    order.id.toString(),
                    gson.toJson(order),
                    "QUANTITY_DECREMENTED"
                ).subscribe()
            }
            .doOnError {
                eventPublisher.publish(
                    "wallet.topic",
                    order.id.toString(),
                    gson.toJson(order),
                    "QUANTITY_UNAVAILABLE"
                ).subscribe()
            }
    }

    override fun run(vararg args: String) {
        warehouseConsumer().subscribe()
    }
}