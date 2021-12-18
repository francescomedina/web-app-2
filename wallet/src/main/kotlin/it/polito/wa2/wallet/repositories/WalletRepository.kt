package it.polito.wa2.wallet.repositories

import it.polito.wa2.wallet.domain.WalletEntity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Mono

interface WalletRepository : ReactiveMongoRepository<WalletEntity, String> {

    fun findByCustomerUsername(customerUsername: String): Mono<WalletEntity?>

}