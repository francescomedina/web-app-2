package it.polito.wa2.wallet.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.exceptions.AppRuntimeException
import it.polito.wa2.api.exceptions.ErrorResponse
import it.polito.wa2.util.gson.GsonUtils
import it.polito.wa2.wallet.OrderEntity
import it.polito.wa2.wallet.ReactiveProducerService
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.wallet.domain.TransactionEntity
import it.polito.wa2.wallet.domain.WalletEntity
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.WalletDTO
import it.polito.wa2.wallet.dto.toTransactionDTO
import it.polito.wa2.wallet.dto.toWalletDTO
import it.polito.wa2.wallet.outbox.OutboxEventPublisher
import it.polito.wa2.wallet.repositories.TransactionRepository
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.Instant

@Service
@Transactional
class WalletServiceImpl(
    val walletRepository: WalletRepository,
    val transactionRepository: TransactionRepository,
    val reactiveProducerService: ReactiveProducerService,
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

    private val logger = LoggerFactory.getLogger(WalletServiceImpl::class.java)

    /**
     * Create a transaction between two wallet (senderWallet to receiverWallet) with a given amount
     * @param userInfoJWT : information about the user that make the request
     * @param transactionDTO : information about the transaction (amount, senderWalletId and receiverWalletId)
     * @return the new transaction created
     */
    @Transactional // Since we update multiple documents we annotated with transactional
    override fun createTransaction(userInfoJWT: UserInfoJWT?, transactionDTO: TransactionDTO, trusted: Boolean, order: OrderEntity?): Mono<TransactionDTO> {
        // Check if the senderWalletId and the receiverWalletId are the same
        logger.info("RICEVENTE: ${transactionDTO.receiverWalletId}")
        logger.info("RICEVENTE: ${transactionDTO.receiverWalletId.toString()}")
        if (transactionDTO.senderWalletId.toString() == transactionDTO.receiverWalletId.toString()) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "The transaction has the same sender and receiver walletId")
        }

        // Check that amount is correct; must be not 0; if the user is normal (non admin) can be only negative transaction
        if (transactionDTO.amount == null || transactionDTO.amount.abs().setScale(2) == BigDecimal("0.0").setScale(2) ) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "The transaction cannot be with amount 0 euro")
        } else if (userInfoJWT!=null && !userInfoJWT.isAdmin() && transactionDTO.amount > BigDecimal("0.0")) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Transaction cannot be with a positive amount (amount must be negative)")
        }

        return Mono.just(transactionDTO.senderWalletId.toString())
            .flatMap(walletRepository::findById)
            .doOnNext {
                logger.info("CI PASSA DA QUI 0")
                if (it == null) {
                    logger.info("CI PASSA DA QUI A")
                    throw AppRuntimeException("Sender wallet not found",HttpStatus.BAD_REQUEST,null)
                }
                if (it.amount < transactionDTO.amount.abs()) {
                    logger.info("CI PASSA DA QUI B")
                    if(trusted){
                        reactiveProducerService.send(
                            ProducerRecord("wallet.topic", null, order?.id.toString(), GsonUtils.gson.toJson(order),listOf(
                                RecordHeader("type", "CREDIT_UNAVAILABLE".toByteArray())
                            ))
                        )
                    }
                    throw AppRuntimeException("Sender has not enough money to compute the transaction",HttpStatus.BAD_REQUEST,null)
                }
                if (!trusted && userInfoJWT!=null && it.customerUsername != userInfoJWT.username) {
                    logger.info("CI PASSA DA QUI C $trusted $userInfoJWT ${it.customerUsername} ${userInfoJWT.username}")
                    throw AppRuntimeException("Only the wallet owner can create transaction",HttpStatus.BAD_REQUEST,null)
                }
                it.amount -= transactionDTO.amount.abs()
                logger.info("AMOUNT BANCA: ${it.amount}")
            }
            .flatMap(walletRepository::save)
            .doOnNext {
                logger.info("CI PASSA DA QUI 1")
                if (it == null) {
                    throw AppRuntimeException("Sender wallet update error",HttpStatus.INTERNAL_SERVER_ERROR,null)
                }
            }
            .flatMap { senderWallet ->
                logger.info("CI PASSA DA QUI 2")
                walletRepository.findById(transactionDTO.receiverWalletId.toString())
                    .doOnNext { receiverWallet ->
                        if (receiverWallet == null) {
                            throw AppRuntimeException("Receiver wallet not found",HttpStatus.BAD_REQUEST,null)
                        }
                        receiverWallet.amount += transactionDTO.amount.abs()
                        val w = walletRepository.save(receiverWallet)
                            .onErrorResume { throw AppRuntimeException("Receiver wallet update error",HttpStatus.INTERNAL_SERVER_ERROR,null) }
                            .subscribe()
                        logger.info("CI PASSA DA QUI 3 ${w}")
                    }
                    .flatMap {
                        val t = transactionRepository.save(TransactionEntity(
                            amount = transactionDTO.amount,
                            time = Instant.now(),
                            senderWalletId = senderWallet.id!!,
                            receiverWalletId = it.id!!,
                            reason = transactionDTO.reason
                        ))
                        logger.info("CI PASSA DA QUI 3 ${t}")
                        t
                    }
                    .onErrorResume { e -> throw AppRuntimeException("Transaction not persisted",HttpStatus.INTERNAL_SERVER_ERROR,e) }
            }
            .onErrorResume { e -> throw AppRuntimeException(e.message,HttpStatus.BAD_REQUEST,e) }
            .map {
                it.toTransactionDTO()
            }
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