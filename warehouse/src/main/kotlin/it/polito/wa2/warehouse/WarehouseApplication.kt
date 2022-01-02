package it.polito.wa2.warehouse

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.transaction.ReactiveTransactionManager
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionalOperator

@SpringBootApplication
@ComponentScan("it.polito.wa2")
@EnableTransactionManagement
class WarehouseApplication {

    @Bean
    fun transactionalOperator(tmx: ReactiveMongoTransactionManager) : TransactionalOperator {
        return TransactionalOperator.create(tmx)
    }

    @Bean
    fun transactionManager(dbf: ReactiveMongoDatabaseFactory) : ReactiveMongoTransactionManager {
        return ReactiveMongoTransactionManager(dbf)
    }
}

fun main(args: Array<String>) {
    runApplication<WarehouseApplication>(*args)
}
