package it.polito.wa2.wallet

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.api.exceptions.AppRuntimeException
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.outbox.OutboxEventPublisher
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.wallet.services.WalletServiceImpl
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
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

data class OrderEntity(
    @JsonProperty("id")
    var id: ObjectId,
    @JsonProperty("status")
    var status: String? = null,
    @JsonProperty("buyer")
    var buyer: String,
    @JsonProperty("products")
    var products: List<ProductEntity> = emptyList(),
)

data class Response(
    @JsonProperty("order")
    var order: OrderEntity,
    @JsonProperty("response")
    var response: String
)


@Component
@Transactional
class WalletEventListener(
    val walletService: WalletServiceImpl,
    val eventPublisher: OutboxEventPublisher,
    val walletRepository: WalletRepository
) {
    private val logger = LoggerFactory.getLogger(WalletEventListener::class.java)
    private val bankId = ObjectId("61be08d04e6ebd990b0fa5db")

    /**
     * TRANSACTIONAL: changes are committed if no exceptions are generated, rollback otherwise
     */
    fun pay(order: OrderEntity, payload: String, isRefund: Boolean = false) : Mono<TransactionDTO> {
        return Mono.just(order)
            .flatMap { walletRepository.findByCustomerUsername(it.buyer) }
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

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(@Payload payload: String) {

        val response = gson.fromJson(payload, Response::class.java)
        logger.info("WALLET Received: ${response.response}")

        if(response.response == "QUANTITY_AVAILABLE"){
            pay(response.order,payload)
        }else if(response.response == "QUANTITY_INCREMENTED"){ //ORDER CANCELED OR QUANTITY NO MORE AVAILABLE
            pay(response.order,payload,true)
        }
    }
}
