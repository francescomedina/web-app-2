package it.polito.wa2.wallet

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import org.springframework.stereotype.Service


@Service
class ReactiveProducerService(
    val reactiveKafkaProducerTemplate: ReactiveKafkaProducerTemplate<String, String>
) {
    private val log = LoggerFactory.getLogger(ReactiveProducerService::class.java)

    fun send(payload: ProducerRecord<String,String>) {
        log.info("send to {}={},", String::class.java.simpleName, payload)
        reactiveKafkaProducerTemplate.send(payload)
            .doOnSuccess { senderResult -> log.info("sent {} offset : {}", payload, senderResult.recordMetadata().offset()) }
        .subscribe()
    }
}