package it.polito.wa2.warehouse

import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class ErrorProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>
) {

    fun produce(topicTarget: String, key: String, value: String) {
        val record = ProducerRecord(topicTarget, key, value)
        kafkaTemplate.send(record)
    }

}