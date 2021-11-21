package it.polito.wa2.wallet.repositories

import it.polito.wa2.wallet.domain.WalletEntity
import org.springframework.data.mongodb.repository.ReactiveMongoRepository

interface WalletRepository : ReactiveMongoRepository<WalletEntity, String>