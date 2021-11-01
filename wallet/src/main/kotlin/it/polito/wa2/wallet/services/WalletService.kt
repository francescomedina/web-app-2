package it.polito.wa2.wallet.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.wallet.domain.WalletEntity
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.WalletDTO
import reactor.core.publisher.Mono
import java.util.*

interface WalletService {
    fun createWallet(userInfoJWT: UserInfoJWT, username: String): Mono<WalletDTO>
    suspend fun getWalletById(userInfoJWT: UserInfoJWT, walletId: String): WalletDTO
    fun createTransaction(userId: Long,transactionDTO: TransactionDTO): TransactionDTO?

    fun getAllWalletTransactions(walletId: Long) : Iterable<TransactionDTO>
    fun getTransactionsByPeriod(walletId: Long, start: Date, end: Date): Iterable<TransactionDTO>
    fun getTransactionByIdAndWalletId(id: Long, wallet_id: Long): TransactionDTO?
}