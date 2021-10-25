package it.polito.wa2.catalog.persistence

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*


interface EmailVerificationTokenRepository : ReactiveMongoRepository<EmailVerificationToken, String> {
    fun findByToken(token: String): Mono<EmailVerificationToken?>

    fun findAllByExpiryDateBefore(expiryDate: Date): Flux<EmailVerificationToken>
}

