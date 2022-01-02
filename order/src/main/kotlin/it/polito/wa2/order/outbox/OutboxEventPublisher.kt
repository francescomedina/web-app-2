package it.polito.wa2.order.outbox

import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.lang.RuntimeException

@Component
class OutboxEventPublisher @Autowired constructor(
	outboxEventRepository: OutboxEventRepository,
	objectMapper: ObjectMapper
){
	private val outboxEventRepository: OutboxEventRepository
	private val objectMapper: ObjectMapper

	init {
	    this.outboxEventRepository = outboxEventRepository
		this.objectMapper = objectMapper
	}

	@Transactional
	suspend fun publish(channel: String?, aggregateId: String, payload: String, type: String) {
//			val payload = objectMapper.writeValueAsString(wallet)
//		val outboxEvent = OutboxEvent(channel = channel!!, messageKey = aggregateId,payload = payload, type = type)
//		val headers: MutableMap<String, String> = HashMap()
//		headers["aggregate_id"] = aggregateId
//		headers["message_id"] = outboxEvent.messageId.toString()
//		headers["type"] = type
//		val encodedHeaders = objectMapper.writeValueAsString(headers)
//		outboxEvent.headers = encodedHeaders
//		outboxEventRepository.save(outboxEvent).onErrorResume {
//			throw RuntimeException(it)
//		}.awaitSingle()
		throw RuntimeException("OUTBOX RUNTIMEEXECPTION LAUNCHED")
	}
}