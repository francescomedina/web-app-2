package it.polito.wa2.wallet.persistence

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface WalletRepository : ReactiveMongoRepository<WalletEntity, String> {

    fun findByOrderId(orderId: Int): Mono<WalletEntity?>
}