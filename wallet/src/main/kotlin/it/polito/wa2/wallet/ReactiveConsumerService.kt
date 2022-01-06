package it.polito.wa2.wallet

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.api.exceptions.AppRuntimeException
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.outbox.OutboxEventPublisher
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.wallet.services.WalletServiceImpl
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
import reactor.core.publisher.Mono
import java.math.BigDecimal

data class Result(
    @JsonProperty("order")
    val order: OrderEntity,
    @JsonProperty("response")
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
    val walletService: WalletServiceImpl,
    val eventPublisher: OutboxEventPublisher,
    val walletRepository: WalletRepository
) : CommandLineRunner {

    var log = LoggerFactory.getLogger(ReactiveConsumerService::class.java)
    private val bankId = ObjectId("61be08d04e6ebd990b0fa5db")

    private fun walletConsumer(): Flux<ConsumerRecord<String, String>> {
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
                val response = gson.fromJson(it.value(), Result::class.java)
                if(it.headers().any { h -> h.key().toString() == "type" && h.value().toString() == "QUANTITY_AVAILABLE" }){
                    pay(response.order,it.value()).subscribe()
                }
                else if(it.headers().any { h -> h.key().toString() == "type" && h.value().toString() == "QUANTITY_UNAVAILABLE" }){
                    pay(response.order,it.value()).subscribe()
                }
                log.info("successfully consumed {}={}", GenericMessage::class.java.simpleName, response)
            }
            .doOnError { throwable: Throwable ->
                log.error("something bad happened while consuming : {}", throwable.message)
            }
    }

    @Transactional
    fun pay(order: OrderEntity, payload: String, isRefund: Boolean = false) : Mono<TransactionDTO> {
        return Mono.just(order)
            .flatMap { walletRepository.findByCustomerUsername(it.buyer!!) }
            .flatMap {
                walletService.createTransaction(
                    null,
                    TransactionDTO(
                        amount = it?.amount,
                        senderWalletId = if(isRefund) bankId else it?.id,
                        receiverWalletId = if(isRefund) it?.id else bankId,
                        reason = if(isRefund) "Order Refund" else "Order Payment"
                    ), true
                )
            }
            .doOnError {
                eventPublisher.publish(
                    "order.topic",
                    order.id.toString(),
                    payload,
                    "TRANSACTION_ERROR"
                ).subscribe()
            }
            .doOnNext {
                eventPublisher.publish(
                    "wallet.topic",
                    order.id.toString(),
                    payload,
                    if(isRefund) "REFUND_TRANSACTION_SUCCESS" else "TRANSACTION_SUCCESS"
                ).subscribe()
            }
            .onErrorResume { Mono.error(AppRuntimeException(it.message, HttpStatus.INTERNAL_SERVER_ERROR,it)) }
    }

    override fun run(vararg args: String) {
        walletConsumer().subscribe()
    }
}