package it.polito.wa2.wallet

import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.outbox.OutboxEventPublisher
import it.polito.wa2.wallet.services.WalletServiceImpl
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class WalletEventListener @Autowired constructor(
    walletService: WalletServiceImpl,
    eventPublisher: OutboxEventPublisher
) {
    private val logger = LoggerFactory.getLogger(WalletEventListener::class.java)
    private val walletService: WalletServiceImpl
    private val eventPublisher: OutboxEventPublisher

    init {
        this.walletService = walletService
        this.eventPublisher = eventPublisher
    }

    @KafkaListener(topics = ["\${topics.in}"])
    suspend fun listen(message: Message<String>) {
        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
        logger.info("Received: ${message.payload}")

        eventPublisher.publish("event.transaction-success", "123", message.payload, "TRANSACTION_CREATED")
        walletService.createTransaction(
            null,
            TransactionDTO(
                amount = BigDecimal(12),
                senderWalletId = ObjectId(),
                receiverWalletId = ObjectId()
            )
            ,true
        ).let {
            eventPublisher.publish("event.transaction-success", "123", message.payload, "TRANSACTION_CREATED")
        }
    }

//    @KafkaListener(topics = ["\${topics.in-error}"])
//    fun error(message: Message<String>) {
////        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
////        logger.info("Received: ${message.payload}")
//        exampleService.rollbackPayment(ExampleEntity("Credit Increased"))
//    }
}
