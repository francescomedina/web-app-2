package it.polito.wa2.order

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component

@Component
class OrderEventListener {

    private val logger = LoggerFactory.getLogger(OrderEventListener::class.java)

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(message: Message<String>) {
        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
        logger.info("Received: ${message.payload}")
    }

    @KafkaListener(topics = ["warehouse.topic"])
    fun orderConfirmed(record: ProducerRecord<String,String>) {
        val value = record.value()
        logger.info("Received: $value")
    }
}
