package it.polito.wa2.wallet.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.wallet.domain.TransactionEntity
import it.polito.wa2.wallet.domain.WalletEntity
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.WalletDTO
import it.polito.wa2.wallet.dto.toTransactionDTO
import it.polito.wa2.wallet.dto.toWalletDTO
import it.polito.wa2.wallet.outbox.OutboxEventPublisher
import it.polito.wa2.wallet.repositories.TransactionRepository
import it.polito.wa2.wallet.utils.ObjectIdTypeAdapter
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant

@Service
class WalletServiceImpl(
    val walletRepository: WalletRepository,
    val transactionRepository: TransactionRepository,
    val eventPublisher: OutboxEventPublisher
) : WalletService {

    /**
     * Create a wallet associated to a username with no money
     * @param userInfoJWT : information about the user that make the request
     * @param username : username associated to that wallet
     * @return the wallet created
     */
    override fun createWallet(userInfoJWT: UserInfoJWT, username: String): Mono<WalletDTO> {

        // Check that the logged-in username is equal to the one that we associate the wallet to
        // (i.e. a user cannot create a wallet for another person)
        if (userInfoJWT.username == username) {
            val newWallet = WalletEntity(customerUsername = userInfoJWT.username)

            return walletRepository.save(newWallet).map {
                it.toWalletDTO()
            }
        }

        throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can't create wallet for another person")

    }

    /**
     * Retrieve the wallet by the WalletId
     * @param userInfoJWT : information about the user that make the request
     * @param walletId : id of the wallet
     * @return the wallet information
     */
    override suspend fun getWalletById(userInfoJWT: UserInfoJWT, walletId: ObjectId): WalletDTO {
        // Take the information about the wallet with that walletID
        val wallet = walletRepository.findById(walletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Wallet not found")

        // The wallet exists, so we check if the user can see that information (only if it is his wallet or is an admin)
        if (wallet.customerUsername == userInfoJWT.username || userInfoJWT.isAdmin()) {
            return wallet.toWalletDTO()
        }

        // User has no permission to see this wallet
        throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see this wallet")
    }

    /**
     * Create a transaction between two wallet (senderWallet to receiverWallet) with a given amount
     * @param userInfoJWT : information about the user that make the request
     * @param transactionDTO : information about the transaction (amount, senderWalletId and receiverWalletId)
     * @return the new transaction created
     */
    @Transactional // Since we update multiple documents we annotated with transactional
    override suspend fun createTransaction(userInfoJWT: UserInfoJWT?, transactionDTO: TransactionDTO, trusted: Boolean): TransactionDTO {
        // Check if the senderWalletId and the receiverWalletId are the same
        if (transactionDTO.senderWalletId.toString() == transactionDTO.receiverWalletId.toString()) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "The transaction has the same sender and receiver walletId")
        }

        // Check that amount is correct; must be not 0; if the user is normal (non admin) can be only negative transaction
        if (transactionDTO.amount == null || transactionDTO.amount.abs().setScale(2) == BigDecimal("0.0").setScale(2) ) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "The transaction cannot be with amount 0 euro")
        } else if (userInfoJWT!=null && !userInfoJWT.isAdmin() && transactionDTO.amount > BigDecimal("0.0")) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Transaction cannot be with a positive amount (amount must be negative)")
        }

        // Check if the senderWalletId and receiverWalletId are valid id
        val senderWallet = walletRepository.findById(transactionDTO.senderWalletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Sender wallet not found")

        val receiverWallet = walletRepository.findById(transactionDTO.receiverWalletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Receiver wallet not found")

        // Check if the owner of the senderWallet is the same of the JWT
        if (!trusted && userInfoJWT!=null && senderWallet.customerUsername != userInfoJWT.username) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Only the wallet owner can create transaction")
        }


        // Check if the sender has enough money to carry out the transaction
        if (senderWallet.amount < transactionDTO.amount.abs()) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Sender has not enough money to compute the transaction")
        }

        // Additional check that the required fields didn't change
        if (senderWallet.id != null && receiverWallet.id != null) {

            // Amount can be positive or negative based on if the transaction is made by the Admin or by the Customer
            // With Abs I can include both cases
            senderWallet.amount -= transactionDTO.amount.abs()
            receiverWallet.amount += transactionDTO.amount.abs()

            walletRepository.save(senderWallet).onErrorMap {
                throw ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error")
            }.awaitSingle()

            walletRepository.save(receiverWallet).onErrorMap {
                throw ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error")
            }.awaitSingle()

            val newTransaction = TransactionEntity(
                amount = transactionDTO.amount,
                time = Instant.now(),
                senderWalletId = senderWallet.id,
                receiverWalletId = receiverWallet.id,
                reason = transactionDTO.reason
            )

//            return transactionRepository.save(newTransaction).awaitSingle().toTransactionDTO()
            val transactionCreated = transactionRepository.save(newTransaction).onErrorMap {
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "TRANSACTION NOT PERSISTED")
            }.awaitSingleOrNull()
            val gson: Gson = GsonBuilder().registerTypeAdapter(ObjectId::class.java, ObjectIdTypeAdapter()).create()
            transactionCreated?.let {
                eventPublisher.publish(
                    "wallet.topic",
                    transactionCreated.id.toString(),
                    gson.toJson(transactionCreated),
                    "TRANSACTION SUCCESS"
                )
                return it.toTransactionDTO()
            }
        }

        throw ErrorResponse(HttpStatus.BAD_REQUEST, "Generic Error")
    }

    /**
     * Return all the transaction involved with a 'walletId' and in a given period (from 'start' to 'end')
     * @param userInfoJWT : information about the user that make the request
     * @param walletId : id of the wallet
     * @param start : start date in millis
     * @param end : end date in millis
     * @return the list of transaction of that walletId and in that period
     */
    override suspend fun getTransactionsByPeriod(
        userInfoJWT: UserInfoJWT,
        walletId: ObjectId,
        start: Instant,
        end: Instant
    ): Flux<TransactionEntity?> {

        // Check if the walletId is valid
        val wallet = walletRepository.findById(walletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Wallet not found")

        // Check that the logged-in user is the owner of the wallet requested or is an admin (can see others wallet)
        if (userInfoJWT.username == wallet.customerUsername || userInfoJWT.isAdmin()) {

            return transactionRepository.findAllByTimeBetweenAndSenderWalletIdOrReceiverWalletId(
                start,
                end,
                walletId,
                walletId
            )
        }

        // User has no permission to see the transaction associate to that walletId
        throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see these transactions")
    }

    /**
     * Get the transaction with a specific id and associated to that a walletId
     * @param userInfoJWT : information about the user that make the request
     * @param transactionId : id of the transaction
     * @param walletId : id of the wallet associated to that transaction
     * @return the transaction with that transactionId
     */
    override suspend fun getTransactionByIdAndWalletId(
        userInfoJWT: UserInfoJWT,
        transactionId: ObjectId,
        walletId: ObjectId
    ): TransactionDTO {

        // Check if the walletId and the transactionId are valid
        val wallet = walletRepository.findById(walletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Wallet not found")

        val transaction = transactionRepository.findById(transactionId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Transaction not found")


        // Check that the logged-in user is the owner of the wallet requested or is an admin (can see others wallet)
        if (userInfoJWT.username == wallet.customerUsername || userInfoJWT.isAdmin()) {

            // Check that the requested transaction belongs to the wallet or is an admin
            if (transaction.senderWalletId == wallet.id || transaction.receiverWalletId == wallet.id || userInfoJWT.isAdmin()) {
                return transaction.toTransactionDTO()
            }
        }

        throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see this transaction")

    }
}