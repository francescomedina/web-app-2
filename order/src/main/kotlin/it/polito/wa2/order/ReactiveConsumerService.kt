package it.polito.wa2.order

import it.polito.wa2.api.exceptions.AppRuntimeException
import it.polito.wa2.order.domain.OrderEntity
import it.polito.wa2.order.dto.toOrderDTO
import it.polito.wa2.order.repositories.OrderRepository
import it.polito.wa2.order.services.MailService
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux

@Service
class ReactiveConsumerService(
    val reactiveKafkaConsumerTemplate: ReactiveKafkaConsumerTemplate<String, String>,
    val orderRepository: OrderRepository,
    val mailService: MailService,
) : CommandLineRunner {

    private val adminEmail = "marco.lg1997@gmail.com"
    var log = LoggerFactory.getLogger(ReactiveConsumerService::class.java)
    private val types = listOf("QUANTITY_DECREMENTED","REFUND_TRANSACTION_SUCCESS", "CREDIT_UNAVAILABLE", "TRANSACTION_ERROR","QUANTITY_UNAVAILABLE_NOT_PURCHASED")

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
                    val order: OrderEntity
                    var status = when (type) {
                        "QUANTITY_DECREMENTED" -> {
                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)
                            "ISSUED"
                        }
                        "QUANTITY_UNAVAILABLE_NOT_PURCHASED" -> {
                            log.info("TYPE ${it.value()}")
                            order = gson.fromJson(it.value(), OrderEntity::class.java)
                            log.info("TYPE $order")
                            "FAILED-QUANTITY_UNAVAILABLE"
                        }
                        "REFUND_TRANSACTION_SUCCESS" -> {
                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)
                            "CANCELED"
                        }
                        "CREDIT_UNAVAILABLE" -> {
//                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
//                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)
                            order = gson.fromJson(it.value(), OrderEntity::class.java)
                            "FAILED-CREDIT_UNAVAILABLE"
                        }
                        "TRANSACTION_ERROR" -> {
                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)
                            "FAILED-TRANSACTION_ERROR"
                        }
                        "REFUND_TRANSACTION_ERROR" -> {
                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)
                            "FAILED-REFUND_TRANSACTION_ERROR"
                        }
                        else -> {
                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)
                            ""
                        }
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
                            if(!order.delivery.isNullOrEmpty()){
                                o.delivery = order.delivery
                            }
                        }
                        .flatMap(orderRepository::save)
                        .doOnNext {
                            o ->
                                listOf(o.buyer, adminEmail).forEach { to ->
                                    mailService.sendMessage(to.toString(), "Order ${o.id.toString()} $status", "Order ${o.id.toString()} has status $status. User ${o.buyer.toString()}")
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