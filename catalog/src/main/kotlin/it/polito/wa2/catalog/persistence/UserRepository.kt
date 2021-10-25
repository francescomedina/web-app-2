package it.polito.wa2.catalog.persistence

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.security.core.userdetails.User
import reactor.core.publisher.Mono

interface UserRepository : ReactiveMongoRepository<UserEntity, String> {
    fun findByUsername(username: String): Mono<UserEntity?>
}