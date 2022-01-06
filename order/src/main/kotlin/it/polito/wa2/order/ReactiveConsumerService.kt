package it.polito.wa2.order

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.api.exceptions.AppRuntimeException
import it.polito.wa2.order.domain.ProductEntity
import it.polito.wa2.order.dto.OrderDTO
import it.polito.wa2.order.dto.toOrderDTO
import it.polito.wa2.order.repositories.OrderRepository
import it.polito.wa2.order.services.MailService
import it.polito.wa2.order.services.OrderServiceImpl
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux

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
    val orderRepository: OrderRepository,
    val mailService: MailService,
) : CommandLineRunner {

    var log = LoggerFactory.getLogger(ReactiveConsumerService::class.java)
    private val types = listOf("QUANTITY_DECREMENTED","QUANTITY_UNAVAILABLE","REFUND_TRANSACTION_SUCCESS", "CREDIT_UNAVAILABLE", "TRANSACTION_ERROR")

    @Transactional
    fun orderConsumer(): Flux<ConsumerRecord<String, String>> {
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
                log.info("TYPE $type")
                if(types.contains(type)){
                    val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                    val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
                    log.info("ORDER $order")
                    val status = when (type) {
                        "QUANTITY_DECREMENTED" -> "ISSUED"
                        "QUANTITY_UNAVAILABLE" -> "FAILED-QUANTITY_UNAVAILABLE"
                        "REFUND_TRANSACTION_SUCCESS" -> "CANCELED"
                        "CREDIT_UNAVAILABLE" -> "FAILED-CREDIT_UNAVAILABLE"
                        "TRANSACTION_ERROR" -> "FAILED-TRANSACTION_ERROR"
                        else -> ""
                    }
                    log.info("TYPE $type")
                    orderRepository.findById(order.id.toString())
                        .onErrorResume { e ->
                            throw AppRuntimeException("Update order error", HttpStatus.INTERNAL_SERVER_ERROR,e)
                        }
                        .doOnNext { o ->
                            if(o==null){
                                throw AppRuntimeException("Order not found", HttpStatus.BAD_REQUEST,o)
                            }
                            o.status = status
                        }
                        .flatMap(orderRepository::save)
                        .doOnNext {
                            o -> if(status == "ISSUED"){
                                mailService.sendMessage(o.buyer!!, "Order Issued", "Order was successfully issued")
                            }
                        }
                        .map { o -> o.toOrderDTO() }
                        .subscribe()
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