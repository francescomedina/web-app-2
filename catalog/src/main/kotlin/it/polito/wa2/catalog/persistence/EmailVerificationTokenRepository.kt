package it.polito.wa2.catalog.persistence

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*


interface EmailVerificationTokenRepository : ReactiveMongoRepository<EmailVerificationToken, String> {
    fun findByToken(token: String): Mono<EmailVerificationToken?>

    fun findAllByExpiryDateBefore(expiryDate: Date): Flux<EmailVerificationToken>
}

