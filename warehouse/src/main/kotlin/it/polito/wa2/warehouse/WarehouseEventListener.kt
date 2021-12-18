package it.polito.wa2.warehouse

import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.messaging.Message
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import java.util.*


@Component
class WarehouseEventListener @Autowired constructor(
    errorProducer: ErrorProducer,
    @Value("\${topics.out}")
    private val topicTarget: String,
    @Value("\${topics.out-error}")
    private val errorTopicTarget: String,
    kafkaTemplate: KafkaTemplate<String, String>
) {
    private val logger = LoggerFactory.getLogger(WarehouseEventListener::class.java)
    private val errorProducer: ErrorProducer
    private val kafkaTemplate: KafkaTemplate<String, String>

    init {
        this.errorProducer = errorProducer
        this.kafkaTemplate = kafkaTemplate
    }

//    fun listener(record: ConsumerRecord<String?, String?>) {
//        println(record.key())
//        println(record.value())
//        println(record.partition())
//        println(record.topic())
//        println(record.offset())
//    }

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(
        @Payload payload: String,
        @Header("aggregate_id") aggregateId: String,
        @Header("message_id") messageId: String,
        @Header("type") type: String
    ) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")

//        if(false){
//            exampleService.addExample(topicTarget,ExampleEntity("Quantity Available"))
//        }else{
//            errorProducer.produce(errorTopicTarget,"123", message.payload)
//        }
        val key = aggregateId + '_' + messageId + '_' + type
        kafkaTemplate.send(ProducerRecord("warehouse.topic", key, payload))
    }

    @KafkaListener(topics = ["wallet.topic"])
    fun decrementQuantity(
        @Payload payload: String,
        @Header("aggregate_id") aggregateId: String,
        @Header("message_id") messageId: String,
        @Header("type") type: String
    ) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")

//        if(false){
//            exampleService.addExample(topicTarget,ExampleEntity("Quantity Available"))
//        }else{
//            errorProducer.produce(errorTopicTarget,"123", message.payload)
//        }
        val key = aggregateId + '_' + messageId + '_' + type
        kafkaTemplate.send(ProducerRecord("warehouse.topic", key, payload))
    }

//    @KafkaListener(topics = ["\${topics.in-error}"])
//    fun error(message: Message<String>) {
////        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
////        logger.info("Received: ${message.payload}")
//
//        if(false){
//            exampleService.addExample(topicTarget,ExampleEntity("Quantity Available"))
//        }else{ // Quantità non disponibile
//            errorProducer.produce("order.topic", "123",message.payload)
//        }
//    }
}
