package it.polito.wa2.catalog.persistence

import org.springframework.data.mongodb.repository.DeleteQuery
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.sql.Timestamp
import java.util.*

interface EmailVerificationTokenRepository : ReactiveMongoRepository<EmailVerificationToken, String> {
    fun findByToken(token: String): Mono<EmailVerificationToken?>

    //@Query(value="{username : $0}", delete = true)
    //@Query("{'expiryDate': { \$lt: ISODate('2022-12-02') }  }") //TODO: This works but when i change the date with $0 or ?#{[0]} doesn't work anymore
    @Query("{'expiryDate': { \$lt: ISODate('$0') }  }")
    fun findMarco(date: String): Mono<EmailVerificationToken>
    //TODO: Doesn't work
}