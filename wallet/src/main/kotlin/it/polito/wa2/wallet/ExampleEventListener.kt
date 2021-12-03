//package it.polito.wa2.wallet
//
//import org.apache.kafka.clients.consumer.ConsumerRecord
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.kafka.annotation.KafkaListener
//import org.springframework.messaging.Message
//import org.springframework.stereotype.Component
//
//@Component
//class ExampleEventListener @Autowired constructor(
//    exampleService: ExampleService
//) {
//    private val logger = LoggerFactory.getLogger(ExampleEventListener::class.java)
//    private val exampleService: ExampleService
//
//    init {
//        this.exampleService = exampleService
//    }
//
//    @KafkaListener(topics = ["\${topics.in}"])
//    fun listen(message: Message<String>) {
//        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
//        logger.info("Received: ${message.payload}")
//
//        exampleService.addExample(ExampleEntity("Credit Reserved"))
//    }
//
//    @KafkaListener(topics = ["\${topics.in-error}"])
//    fun error(message: Message<String>) {
////        message.headers.forEach { header, value -> logger.info("Header $header: $value") }
////        logger.info("Received: ${message.payload}")
//        exampleService.rollbackPayment(ExampleEntity("Credit Increased"))
//    }
//}
