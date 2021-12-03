package it.polito.wa2.wallet

import it.polito.wa2.wallet.services.WalletServiceImpl
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component

@Component
class ExampleEventListener @Autowired constructor(
    walletService: WalletServiceImpl
) {
    private val logger = LoggerFactory.getLogger(ExampleEventListener::class.java)
    private val walletService: WalletServiceImpl

    init {
        this.walletService = walletService
    }

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(message: Message<String>) {
        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
        logger.info("Received: ${message.payload}")

//        walletService.processPayment(ExampleEntity("Credit Reserved"))
    }

//    @KafkaListener(topics = ["\${topics.in-error}"])
//    fun error(message: Message<String>) {
////        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
////        logger.info("Received: ${message.payload}")
//        exampleService.rollbackPayment(ExampleEntity("Credit Increased"))
//    }
}
