package it.polito.wa2.order.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import reactor.kafka.receiver.ReceiverOptions


@Configuration
class ReactiveKafkaConsumerConfig {
    @Bean
    fun kafkaReceiverOptions(
        @Value(value = "\${CONSUMER_TOPIC}") topic: String,
        kafkaProperties: KafkaProperties
    ): ReceiverOptions<String, String> {
        val basicReceiverOptions: ReceiverOptions<String, String> =
            ReceiverOptions.create(kafkaProperties.buildConsumerProperties())
        return basicReceiverOptions.subscription(topic.split(",").toList())
    }

    @Bean
    fun reactiveKafkaConsumerTemplate(kafkaReceiverOptions: ReceiverOptions<String?, String>): ReactiveKafkaConsumerTemplate<String, String> {
        return ReactiveKafkaConsumerTemplate<String, String>(kafkaReceiverOptions)
    }
}