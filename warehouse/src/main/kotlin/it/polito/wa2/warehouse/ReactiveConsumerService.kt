package it.polito.wa2.warehouse

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.util.gson.GsonUtils
import it.polito.wa2.warehouse.outbox.OutboxEventPublisher
import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import it.polito.wa2.warehouse.ReactiveProducerService

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
) : CommandLineRunner {

    var log = LoggerFactory.getLogger(ReactiveConsumerService::class.java)

    @Transactional
    fun checkProductAvailability(order: OrderEntity): Flux<ProductEntity> {
        return Mono.just(order.products)
            .flatMapMany {
                Flux.fromIterable(it)
                    .doOnNext {
                        log.info("ENTRATO 1 $it")
                        productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
                            .doOnNext { p -> Assert.isTrue(p!=null,"Product no more available") }
                            .subscribe()
                    }
                    .doOnError {
                        log.info("ENTRATO 2 $it")
                        reactiveProducerService.send(
                            ProducerRecord("asd.topic", order.id.toString(), GsonUtils.gson.toJson(Result(order,"QUANTITY_UNAVAILABLE")))
                        )
                        throw RuntimeException(it)
                    }
            }
            .doOnComplete {
                log.info("ENTRATO 3")
                reactiveProducerService.send(
                    ProducerRecord(
                        "warehouse.topic",
                        order.id.toString(),
                        GsonUtils.gson.toJson(Result(order,"QUANTITY_AVAILABLE"))
                    )
                )
            }
    }

    private fun consumeFakeConsumerDTO(): Flux<String> {
        return reactiveKafkaConsumerTemplate
            .receiveAutoAck() // .delayElements(Duration.ofSeconds(2L)) // BACKPRESSURE
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
//            .filter { obj: ConsumerRecord<String, String> -> obj.head }
            .map { obj: ConsumerRecord<String, String> -> obj.value() }
            .doOnNext { payload: String? ->
                val genericMessage = GsonUtils.gson.fromJson(payload, GenericMessage::class.java)
                val order = GsonUtils.gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
                checkProductAvailability(order).subscribe()
                log.info("successfully consumed {}={}", GenericMessage::class.java.simpleName, payload)
            }
            .doOnError { throwable: Throwable ->
                log.error("something bad happened while consuming : {}", throwable.message)
            }
    }

    override fun run(vararg args: String) {
        // we have to trigger consumption
        consumeFakeConsumerDTO().subscribe()
    }
}