package it.polito.wa2.wallet

import brave.Tracer
import brave.propagation.B3SingleFormat
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class ExampleEventService(
        private val outboxEventEntityRepository: OutboxEventEntityRepository,
        private val objectMapper: ObjectMapper,
        private val tracer: Tracer
) {

    fun publishEvent(topicTarget: String, event: ExampleEvent) {
        val traceId = B3SingleFormat.writeB3SingleFormat(tracer.currentSpan().context())
        val outboxEvent = OutboxEventEntity(
                destinationTopic = topicTarget,
                aggregateId = event.exampleId,
                type = event.javaClass.simpleName,
                payload = objectMapper.writeValueAsString(event),
                traceId = traceId
        )
        outboxEventEntityRepository.save(outboxEvent)
    }
}
