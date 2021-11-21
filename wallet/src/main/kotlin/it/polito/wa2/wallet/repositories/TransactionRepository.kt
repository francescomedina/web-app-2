package it.polito.wa2.wallet.repositories

import it.polito.wa2.wallet.domain.TransactionEntity
import org.bson.types.ObjectId
import org.springframework.data.mongodb.repository.Query
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import reactor.core.publisher.Flux
import java.time.Instant

interface TransactionRepository : ReactiveMongoRepository<TransactionEntity, String> {

    fun findAllByTimeBetweenAndSenderWalletIdOrReceiverWalletId(
        from: Instant?,
        to: Instant?,
        senderId: ObjectId,
        receiverId: ObjectId
    ): Flux<TransactionEntity?>

}