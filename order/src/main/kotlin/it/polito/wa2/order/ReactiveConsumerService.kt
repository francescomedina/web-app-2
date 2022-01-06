package it.polito.wa2.order

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.order.domain.ProductEntity
import it.polito.wa2.order.dto.OrderDTO
import it.polito.wa2.order.dto.toOrderDTO
import it.polito.wa2.order.services.OrderServiceImpl
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

data class Result(
    @JsonProperty("order")
    val order: OrderEntity,
    @JsonProperty("response")
    val response: String
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
    val orderService: OrderServiceImpl
) : CommandLineRunner {

    var log = LoggerFactory.getLogger(ReactiveConsumerService::class.java)
    private val types = listOf("QUANTITY_DECREMENTED","QUANTITY_UNAVAILABLE","REFUND_TRANSACTION_SUCCESS", "CREDIT_UNAVAILABLE", "TRANSACTION_ERROR")

    private fun orderConsumer(): Flux<ConsumerRecord<String, String>> {
        return reactiveKafkaConsumerTemplate
            .receiveAutoAck()
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
                val type = it.headers().reduce { _, header -> if(header.key() == "type") header else null}?.value().toString()
                if(types.contains(type)){
                    val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                    val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
                    val status = when (type) {
                        "QUANTITY_DECREMENTED" -> "ISSUED"
                        "QUANTITY_UNAVAILABLE" -> "FAILED-QUANTITY_UNAVAILABLE"
                        "REFUND_TRANSACTION_SUCCESS" -> "CANCELED"
                        "CREDIT_UNAVAILABLE" -> "FAILED-CREDIT_UNAVAILABLE"
                        "TRANSACTION_ERROR" -> "FAILED-TRANSACTION_ERROR"
                        else -> ""
                    }
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
                    log.info("successfully consumed {}={}", GenericMessage::class.java.simpleName, it.value())
                }
            }
            .doOnError { throwable: Throwable ->
                log.error("something bad happened while consuming : {}", throwable.message)
            }
    }

    override fun run(vararg args: String) {
        orderConsumer().subscribe()
    }
}