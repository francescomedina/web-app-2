//package it.polito.wa2.warehouse.config
//
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.data.mongodb.MongoDatabaseFactory
//import org.springframework.data.mongodb.MongoTransactionManager
//import org.springframework.stereotype.Component
//
//@Configuration
//class MongoConfig {
//    /**
//     * Enable transaction support for MongoDB (disabled by default).
//     *
//     * @see [Spring Data MongoDB Transactions](https://docs.spring.io/spring-data/mongodb/docs/current/reference/html/.mongo.transactions)
//     */
//    @Bean
//    fun mongoTransactionManager(mongoDatabaseFactory: MongoDatabaseFactory?): MongoTransactionManager {
//        return MongoTransactionManager(mongoDatabaseFactory!!)
//    }
//}
