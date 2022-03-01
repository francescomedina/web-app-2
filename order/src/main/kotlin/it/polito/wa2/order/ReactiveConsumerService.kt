package it.polito.wa2.order

import it.polito.wa2.api.exceptions.AppRuntimeException
import it.polito.wa2.order.controllers.OrderController
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

    private val log = LoggerFactory.getLogger(ReactiveConsumerService::class.java)

    private val types = listOf(
        "QUANTITY_DECREMENTED",
        "REFUND_TRANSACTION_SUCCESS",
        "CREDIT_UNAVAILABLE",
        "TRANSACTION_ERROR",
        "QUANTITY_UNAVAILABLE_NOT_PURCHASED"
    )

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
                val type = String(it.headers().reduce { _, header -> if (header.key() == "type") header else null }
                    ?.value() as ByteArray)
                log.info("TYPE $type")
                if (types.contains(type)) {
                    val order: OrderEntity
                    var status = when (type) {
                        "QUANTITY_DECREMENTED" -> {
                            log.info("SAGA-ORDER: Reading QUANTITY_DECREMENTED from warehouse.topic")

                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)

                            "ISSUED"
                        }
                        "QUANTITY_UNAVAILABLE_NOT_PURCHASED" -> {
                            log.info("SAGA-ORDER: Reading QUANTITY_UNAVAILABLE_NOT_PURCHASED from warehouse.topic")

                            order = gson.fromJson(it.value(), OrderEntity::class.java)

                            "FAILED-QUANTITY_UNAVAILABLE"
                        }
                        "REFUND_TRANSACTION_SUCCESS" -> {
                            log.info("SAGA-ORDER: Reading REFUND_TRANSACTION_SUCCESS from warehouse.topic")

                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)

                            "CANCELED"
                        }
                        "CREDIT_UNAVAILABLE" -> {
                            log.info("SAGA-ORDER: Reading CREDIT_UNAVAILABLE from wallet.topic")

                            order = gson.fromJson(it.value(), OrderEntity::class.java)

                            "FAILED-CREDIT_UNAVAILABLE"
                        }
                        "TRANSACTION_ERROR" -> {
                            log.info("SAGA-ORDER: Reading TRANSACTION_ERROR from wallet.topic")

                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)

                            "FAILED-TRANSACTION_ERROR"
                        }
                        "REFUND_TRANSACTION_ERROR" -> {
                            log.info("SAGA-ORDER: Reading REFUND_TRANSACTION_ERROR from wallet.topic")

                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)

                            "FAILED-REFUND_TRANSACTION_ERROR"
                        }
                        else -> {
                            log.error(
                                "SAGA-ORDER: Unknown message type from warehouse.topic ${
                                    String(
                                        it.headers()
                                            .reduce { _, header -> if (header.key() == "type") header else null }
                                            ?.value() as ByteArray
                                    )
                                }"
                            )

                            val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                            order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)

                            ""
                        }
                    }

                    orderRepository.findById(order.id.toString())
                        .onErrorResume { e ->
                            log.error("Order/ReactiveConsumerService: Error during retrieving order details")
                            throw AppRuntimeException("Update order error", HttpStatus.INTERNAL_SERVER_ERROR, e)
                        }
                        .doOnNext { o ->
                            if (o == null) {
                                log.error("Order/ReactiveConsumerService: Error order with id ${order.id} not found")
                                throw AppRuntimeException("Order not found", HttpStatus.BAD_REQUEST, null)
                            }
                            o.status = status
                            if (!order.delivery.isNullOrEmpty()) {
                                o.delivery = order.delivery
                            }
                        }
                        .flatMap(orderRepository::save)
                        .doOnNext { o ->
                            listOf(o.buyer, adminEmail).forEach { to ->
                                mailService.sendMessage(
                                    to.toString(),
                                    "Order ${o.id.toString()} $status",
                                    "Order ${o.id.toString()} has status $status. User ${o.buyer.toString()}"
                                )
                            }
                        }
                        .map { o -> o.toOrderDTO() }
                        .subscribe()
                    log.info("SAGA-ORDER: Successfully consumed {}={}", GenericMessage::class.java.simpleName, it.value())
                }
            }
            .doOnError { throwable: Throwable ->
                log.error("SAGA-ORDER: Something bad happened while consuming : {}", throwable.message)
            }
    }

    override fun run(vararg args: String) {
        orderConsumer().subscribe()
    }
}