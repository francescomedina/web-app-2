package it.polito.wa2.wallet

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.util.http.HttpErrorInfo
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.toWalletDTO
import it.polito.wa2.wallet.outbox.OutboxEventPublisher
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.wallet.services.WalletServiceImpl
import it.polito.wa2.wallet.utils.ObjectIdTypeAdapter
import kotlinx.coroutines.reactive.awaitFirst
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
        logger.info("WALLET Received: ${response.response}")
        if(response.response == "QUANTITY_AVAILABLE"){
            logger.info("Received Wallet: ${response.order.buyer}")
            runBlocking {
                val senderWallet = walletRepository.findByCustomerUsername(response.order.buyer)?.awaitFirst()
                logger.info("Received Wallet 2: $senderWallet")
                senderWallet?.let {
                    logger.info("Received Wallet 3: ${senderWallet.toWalletDTO()}")
                    logger.info("Received Wallet 4: ${senderWallet.id}")
                    logger.info("Received Wallet 5: ${ObjectId(senderWallet.id.toString())}")
                    logger.info("Received Wallet 6: ${ObjectId("61be08d04e6ebd990b0fa5db")}")
                    logger.info("Received Wallet 7: ${ObjectId("61be08d04e6ebd990b0fa5db").toString()}")
                    val newTransaction = TransactionDTO(
                        amount = BigDecimal(12),
                        senderWalletId = senderWallet.id,
                        receiverWalletId = ObjectId("61be08d04e6ebd990b0fa5db"),
                        reason = "Order Payment"
                    )
                    logger.info("CREATO QUETSO: ${newTransaction}")
                    try {
                        val transactionCreatedDTO = walletService.createTransaction(
                            null,
                            newTransaction
                            ,true
                        ).awaitSingleOrNull()
                        logger.info("FINALE: ${transactionCreatedDTO}")
                        logger.info("FINALE 2: ${transactionCreatedDTO?.id.toString()}")
                        transactionCreatedDTO?.let {
                            eventPublisher.publish(
                                "wallet.topic",
                                transactionCreatedDTO.id.toString(),
                                payload,
                                "TRANSACTION_SUCCESS"
                            )
                        }
                    }catch (e: ErrorResponse){
                        eventPublisher.publish(
                            "wallet.topic",
                            response.order.id.toString(),
                            gson.toJson(response.order),
                            "CREDIT_UNAVAILABLE"
                        )
                    }
                }
            }
        }else if(response.response == "QUANTITY_INCREMENTED"){
            logger.info("WALLET Received 45: ${response}")
            runBlocking {
                val senderWallet = walletRepository.findByCustomerUsername(response.order.buyer)?.awaitFirst()
                logger.info("Received Wallet 2: $senderWallet")
                senderWallet?.let {
                    logger.info("Received Wallet 3: ${senderWallet.toWalletDTO()}")
                    logger.info("Received Wallet 4: ${senderWallet.id}")
                    logger.info("Received Wallet 5: ${ObjectId(senderWallet.id.toString())}")
                    logger.info("Received Wallet 6: ${ObjectId("61be08d04e6ebd990b0fa5db")}")
                    logger.info("Received Wallet 7: ${ObjectId("61be08d04e6ebd990b0fa5db").toString()}")
                    val newTransaction = TransactionDTO(
                        amount = BigDecimal(12),
                        senderWalletId = ObjectId("61be08d04e6ebd990b0fa5db"),
                        receiverWalletId = senderWallet.id,
                        reason = "Order Refund"
                    )
                    val transactionCreatedDTO = walletService.createTransaction(
                        null,
                        newTransaction
                        ,true
                    ).awaitSingleOrNull()
                    logger.info("FINALE: ${transactionCreatedDTO}")
                    logger.info("FINALE 2: ${transactionCreatedDTO?.id.toString()}")
                    logger.info("FINALE 54: ${response.order}")
                    transactionCreatedDTO?.let {
                        eventPublisher.publish(
                            "wallet.topic",
                            response.order.id.toString(),
                            gson.toJson(response.order),
                            "REFUND_TRANSACTION_SUCCESS"
                        )
                    }
                }
            }
        }
    }
}
