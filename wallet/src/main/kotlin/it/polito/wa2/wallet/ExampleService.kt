//package it.polito.wa2.wallet
//
//import org.springframework.beans.factory.annotation.Value
//import org.springframework.stereotype.Service
//import org.springframework.transaction.annotation.Transactional
//import java.time.Instant
//import java.util.*
//
//@Service
//class ExampleService(
//        private val exampleEntityRepository: ExampleEntityRepository,
//        private val exampleEventService: ExampleEventService,
//        @Value("\${topics.out}")
//        private val topicTarget: String,
//        @Value("\${topics.out-error}")
//        private val errorTopicTarget: String
//) {
//
//    @Transactional
//    fun addExample(example: ExampleEntity) {
//        exampleEntityRepository.save(example)
//        exampleEventService.publishEvent(topicTarget,ExampleEvent(example.id, "Wallet added"))
//    }
//
//    @Transactional
//    fun rollbackPayment(entity: ExampleEntity){
//        /// TODO top-up the credit on the user's wallet
//        exampleEntityRepository.save(entity)
//        exampleEventService.publishEvent(errorTopicTarget,ExampleEvent(entity.id, "Wallet rollback"))
//    }
//
//}
