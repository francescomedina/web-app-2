package it.polito.wa2.wallet

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.outbox.OutboxEventPublisher
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.wallet.services.WalletServiceImpl
import it.polito.wa2.wallet.utils.ObjectIdTypeAdapter
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.apache.kafka.clients.producer.ProducerRecord
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.aggregation.MergeOperation.UniqueMergeId.id
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Component
import java.math.BigDecimal

data class ProductEntity(
    @JsonProperty("id")
    var id: ObjectId,
    @JsonProperty("amount")
    var amount: BigDecimal,
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
class WalletEventListener @Autowired constructor(
    walletService: WalletServiceImpl,
    eventPublisher: OutboxEventPublisher,
    walletRepository: WalletRepository
) {
    private val logger = LoggerFactory.getLogger(WalletEventListener::class.java)
    private val walletService: WalletServiceImpl
    private val eventPublisher: OutboxEventPublisher
    private val walletRepository: WalletRepository

    init {
        this.walletService = walletService
        this.eventPublisher = eventPublisher
        this.walletRepository = walletRepository
    }

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(
        @Payload payload: String
    ) {
        val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
        val response = gson.fromJson(payload, Response::class.java)
        logger.info("Received Wallet: $response")
//        val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
//        logger.info("Received2: $order")
        runBlocking {
            val senderWallet = walletRepository.findByCustomerUsername(response.order.buyer).awaitSingleOrNull()
            walletService.createTransaction(
                null,
                TransactionDTO(
                    amount = BigDecimal(124),
                    senderWalletId = senderWallet!!.id,
                    receiverWalletId = ObjectId("61bde6858a44fed22cc86135")
                )
                ,true
            )
        }
    }
}
