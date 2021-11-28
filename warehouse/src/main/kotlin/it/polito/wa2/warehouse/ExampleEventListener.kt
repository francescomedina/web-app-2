package it.polito.wa2.warehouse

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.Message
import org.springframework.stereotype.Component
import java.util.*


@Component
class ExampleEventListener @Autowired constructor(
    exampleService: ExampleService,
    errorProducer: ErrorProducer,
    @Value("\${topics.out}")
    private val topicTarget: String,
    @Value("\${topics.out-error}")
    private val errorTopicTarget: String
) {
    private val logger = LoggerFactory.getLogger(ExampleEventListener::class.java)
    private val exampleService: ExampleService
    private val errorProducer: ErrorProducer

    init {
        this.exampleService = exampleService
        this.errorProducer = errorProducer
    }

//    fun listener(record: ConsumerRecord<String?, String?>) {
//        println(record.key())
//        println(record.value())
//        println(record.partition())
//        println(record.topic())
//        println(record.offset())
//    }

    @KafkaListener(topics = ["\${topics.in}"])
    fun listen(message: Message<String>) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")

        if(false){
            exampleService.addExample(topicTarget,ExampleEntity("Quantity Available"))
        }else{
            errorProducer.produce(errorTopicTarget,"123", message.payload)
        }
    }

    @KafkaListener(topics = ["\${topics.in-error}"])
    fun error(message: Message<String>) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")

        if(false){
            exampleService.addExample(topicTarget,ExampleEntity("Quantity Available"))
        }else{ // Quantità non disponibile
            errorProducer.produce("order.topic", "123",message.payload)
        }
    }
}
