package it.polito.wa2.catalog.persistence

import it.polito.wa2.catalog.domain.UserEntity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface UserRepository : ReactiveMongoRepository<UserEntity, String> {
    fun findByUsername(username: String): Mono<UserEntity?>
}