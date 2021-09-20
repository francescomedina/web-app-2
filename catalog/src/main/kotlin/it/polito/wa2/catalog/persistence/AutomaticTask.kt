package it.polito.wa2.catalog.persistence

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*


@Configuration
@EnableScheduling
class SpringConfig(
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository
) {

    @Scheduled(fixedDelay = 60000)
    fun scheduleFixedDelayTask() {
//Instant.now().toString();


      // val prova = emailVerificationTokenRepository.findMarco("2022-12-02").block()

        println(
            //"Fixed delay task - " + Date(System.currentTimeMillis())
        )
    }

}