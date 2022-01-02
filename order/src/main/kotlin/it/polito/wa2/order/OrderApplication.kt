package it.polito.wa2.order

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.ReactiveMongoTransactionManager
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.transaction.annotation.EnableTransactionManagement
import org.springframework.transaction.reactive.TransactionalOperator
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
@EnableEurekaClient
@ComponentScan("it.polito.wa2")
@RestController
@EnableTransactionManagement
class OrderApplication {

    /** START JAVA MAIL SENDER **/

    @Value("\${spring.mail.host}")
    val host: String? = null
    @Value("\${spring.mail.port}")
    val port: Int? = 0
    @Value("\${spring.mail.username}")
    val username: String? = null
    @Value("\${spring.mail.password}")
    val password: String? = null
    @Value("\${spring.mail.properties.mail.smtp.auth}")
    val auth: String? = null
    @Value("\${spring.mail.properties.mail.smtp.starttls.enable}")
    val enable: String? = null
    @Value("\${spring.mail.properties.mail.debug}")
    val debug: String? = null

    @Bean
    fun getMailSender(): JavaMailSender {

        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port ?: 8080

        mailSender.username = username
        mailSender.password = password

        val props = mailSender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = auth
        props["mail.smtp.starttls.enable"] = enable
        props["mail.debug"] = debug

        return mailSender
    }

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
    runApplication<OrderApplication>(*args)
}
