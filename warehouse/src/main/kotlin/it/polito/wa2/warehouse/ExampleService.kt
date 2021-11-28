package it.polito.wa2.warehouse

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

@Service
class ExampleService(
        private val exampleEntityRepository: ExampleEntityRepository,
        private val exampleEventService: ExampleEventService
) {

    @Transactional
    fun addExample(topicName: String, example: ExampleEntity) {
        exampleEntityRepository.save(example)
        exampleEventService.publishEvent(topicName, ExampleEvent(example.id, "Warehouse added"))
    }

}
