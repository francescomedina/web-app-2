package it.polito.wa2.order.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class OutboxEventPublisher constructor(
    val outboxEventRepository: OutboxEventRepository,
    val objectMapper: ObjectMapper
) {

    fun publish(channel: String, aggregateId: String, payload: String, type: String): Mono<OutboxEvent> {
        val outboxEvent = OutboxEvent(channel = channel, messageKey = aggregateId, payload = payload, type = type)
        val headers: MutableMap<String, String> = HashMap()
        headers["aggregate_id"] = aggregateId
        headers["message_id"] = outboxEvent.messageId.toString()
        headers["type"] = type
        val encodedHeaders = objectMapper.writeValueAsString(headers)
        outboxEvent.headers = encodedHeaders

        return outboxEventRepository.save(outboxEvent)
    }
}