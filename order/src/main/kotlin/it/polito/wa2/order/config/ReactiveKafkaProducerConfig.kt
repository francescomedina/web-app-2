package it.polito.wa2.order.config

import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate
import reactor.kafka.sender.SenderOptions


@Configuration
class ReactiveKafkaProducerConfig {
    @Bean
    fun reactiveKafkaProducerTemplate(
        properties: KafkaProperties
    ): ReactiveKafkaProducerTemplate<String, String> {
        val props = properties.buildProducerProperties()
        return ReactiveKafkaProducerTemplate<String, String>(SenderOptions.create(props))
    }
}