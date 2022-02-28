package it.polito.wa2.catalog.persistence

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import java.util.*


@Configuration
@EnableScheduling
class SpringConfig(
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository
) {

    @Scheduled(fixedDelay = 60000)
    fun scheduleFixedDelayTask() {
        println("----------------------------------------------------")
        println("Fixed task executing")
        val nowInMillis = Calendar.getInstance().timeInMillis

        val idToEliminate = emailVerificationTokenRepository.findAllByExpiryDateBefore(Date(nowInMillis)).collectList().block()

        idToEliminate?.forEach {
            println(
                "Fixed delay task to eliminate token expired -  ${it.id}  "
            )
            emailVerificationTokenRepository.deleteById(it.id.toString()).block()
        }
        println("----------------------------------------------------")

    }


}