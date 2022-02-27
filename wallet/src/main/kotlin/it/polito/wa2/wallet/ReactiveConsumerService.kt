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
import org.slf4j.Logger
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
    val walletService: WalletServiceImpl,
    val eventPublisher: OutboxEventPublisher,
    val walletRepository: WalletRepository
) : CommandLineRunner {

    private val log: Logger = LoggerFactory.getLogger(ReactiveConsumerService::class.java)
    private val bankId = ObjectId("61be08d04e6ebd990b0fa5db")

    private fun walletConsumer(): Flux<ConsumerRecord<String, String>> {
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
                when(String(it.headers().reduce { _, header -> if(header.key() == "type") header else null}?.value() as ByteArray)) {
                    "QUANTITY_AVAILABLE" -> {
                        val order = gson.fromJson(it.value(), OrderEntity::class.java)
                        pay(order).subscribe()
                    }
                    "QUANTITY_UNAVAILABLE" -> {
                        val order = gson.fromJson(it.value(), OrderEntity::class.java)
                        pay(order,true).subscribe()
                    }
                    "WAREHOUSE_PRODUCTS_RETURNED" -> {
                        val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                        val order = gson.fromJson(genericMessage.payload.toString(), OrderEntity::class.java)
                        pay(order,true).subscribe()
                    }
                }
            }
            .doOnError { throwable: Throwable ->
                log.error("something bad happened while consuming : {}", throwable.message)
            }
    }

    @Transactional
    fun pay(order: OrderEntity, isRefund: Boolean = false) : Mono<TransactionDTO> {
        return Mono.just(order)
            .flatMap { walletRepository.findByCustomerUsername(it.buyer!!) }
            .flatMap {
                val tot = order.products
                    .map { p -> p.price.multiply(BigDecimal.valueOf(p.quantity.toDouble())) }
                    .reduce { tot, item -> tot + item }
                walletService.createTransaction(
                    null,
                    TransactionDTO(
                        amount = tot,
                        senderWalletId = if(isRefund) bankId else it?.id,
                        receiverWalletId = if(isRefund) it?.id else bankId,
                        reason = if(isRefund) "Order Refund" else "Order Payment"
                    ), true,
                    order
                )
            }
            .doOnError {
                eventPublisher.publish(
                    "order.topic",
                    order.id.toString(),
                    gson.toJson(order),
                    if(isRefund) "REFUND_TRANSACTION_ERROR" else "TRANSACTION_ERROR"
                ).subscribe()
                if(isRefund){
                    eventPublisher.publish(
                        "wallet.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "REFUND_TRANSACTION_ERROR"
                    ).subscribe()
                }
            }
            .doOnNext {
                eventPublisher.publish(
                    "wallet.topic",
                    order.id.toString(),
                    gson.toJson(order),
                    if(isRefund) "REFUND_TRANSACTION_SUCCESS" else "TRANSACTION_SUCCESS"
                ).subscribe{
                    log.info("successfully consumed ${if(isRefund) "REFUND" else "PAYMENT"} {} ", it)
                }
            }
            .onErrorResume { Mono.error(AppRuntimeException(it.message, HttpStatus.INTERNAL_SERVER_ERROR,it)) }
    }

    override fun run(vararg args: String) {
        walletConsumer().subscribe()
    }
}