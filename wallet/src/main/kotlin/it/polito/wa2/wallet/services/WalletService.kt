package it.polito.wa2.wallet.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.wallet.domain.TransactionEntity
import it.polito.wa2.wallet.domain.WalletEntity
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.WalletDTO
import org.bson.types.ObjectId
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.*

interface WalletService {
    fun createWallet(userInfoJWT: UserInfoJWT, username: String): Mono<WalletDTO>
    suspend fun getWalletById(userInfoJWT: UserInfoJWT, walletId: ObjectId): WalletDTO
    suspend fun createTransaction(userInfoJWT: UserInfoJWT?,transactionDTO: TransactionDTO, trusted: Boolean = false): TransactionDTO?

    suspend fun getTransactionsByPeriod(userInfoJWT: UserInfoJWT, walletId: ObjectId, start: Instant, end: Instant): Flux<TransactionEntity?>
    suspend fun getTransactionByIdAndWalletId(userInfoJWT: UserInfoJWT, transactionId: ObjectId, walletId: ObjectId): TransactionDTO
}