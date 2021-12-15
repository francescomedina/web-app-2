package it.polito.wa2.wallet

import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.outbox.OutboxEventPublisher
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.wallet.services.WalletServiceImpl
import org.apache.kafka.clients.producer.ProducerRecord
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.aggregation.MergeOperation.UniqueMergeId.id
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.math.BigDecimal


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
    suspend fun listen(record: ProducerRecord<String,String>) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")

        logger.info("Received: ${record.value()}")
        walletService.createTransaction(
            null,
            TransactionDTO(
                amount = BigDecimal(124),
                senderWalletId = ObjectId("61b92e028e687220fcce4994"),
                receiverWalletId = ObjectId("61b92e6d3055382b1b4edb7d")
            )
            ,true
        )
//            .let {
//            eventPublisher.publish("event.transaction-success", "123", message.payload, "TRANSACTION_CREATED")
//        }
    }

//    @KafkaListener(topics = ["\${topics.in-error}"])
//    fun error(message: Message<String>) {
////        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
////        logger.info("Received: ${message.payload}")
//        exampleService.rollbackPayment(ExampleEntity("Credit Increased"))
//    }
}
