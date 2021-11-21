package it.polito.wa2.wallet.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.wallet.controllers.ErrorResponse
import it.polito.wa2.wallet.domain.TransactionEntity
import it.polito.wa2.wallet.domain.WalletEntity
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.WalletDTO
import it.polito.wa2.wallet.dto.toTransactionDTO
import it.polito.wa2.wallet.dto.toWalletDTO
import it.polito.wa2.wallet.repositories.TransactionRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Service
//@EnableAutoConfiguration
class WalletServiceImpl(
    val walletRepository: WalletRepository,
    val transactionRepository: TransactionRepository,
) : WalletService {

    override fun createWallet(userInfoJWT: UserInfoJWT, username: String): Mono<WalletDTO> {

        // The username must be the same of the username inside the JWT
        if (userInfoJWT.username == username) {
            val newWallet = WalletEntity(customerUsername = userInfoJWT.username)

            return walletRepository.save(newWallet).map {
                println(it)
                it.toWalletDTO()
            }
        }

        throw ErrorResponse(HttpStatus.BAD_REQUEST, "You can't create wallet for another person")

    }

    override suspend fun getWalletById(userInfoJWT: UserInfoJWT, walletId: String): WalletDTO {
        // Take the information about the wallet with that walletID
        val wallet = walletRepository.findById(walletId).awaitSingleOrNull()

        wallet?.let {
            // The wallet exists, so we check if the user can see that information (only if it is his wallet or admin)
            if (it.customerUsername == userInfoJWT.username || userInfoJWT.isAdmin()) {
                return it.toWalletDTO()
            }
            throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see this wallet")
        }
        // Wallet is null
        throw ErrorResponse(HttpStatus.NOT_FOUND, "Wallet not found")
    }

    // Since we update multiple documents we annotated with transactional
    @Transactional
    override suspend fun createTransaction(userInfoJWT: UserInfoJWT, transactionDTO: TransactionDTO): TransactionDTO? {
        if (transactionDTO.senderWalletId.toString() == transactionDTO.receiverWalletId.toString()) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "The sender id and receiver id are the same")
        }

        // Check if the senderWalletId and receiverWalletId are valid id
        val senderWallet = walletRepository.findById(transactionDTO.senderWalletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Wallet not found")

        val receiverWallet = walletRepository.findById(transactionDTO.receiverWalletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Wallet not found")

        // Check if the owner of the senderWallet is the same of the JWT
        if (senderWallet.customerUsername != userInfoJWT.username) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Only the wallet owner can create transaction")
        }

        // Check if the sender has enough money to carry out the transaction
        if (senderWallet.amount < transactionDTO.amount) {
            throw ErrorResponse(HttpStatus.BAD_REQUEST, "Sender has not enough money to compute the transaction")
        }

        // Additional check that the required fields didn't change
        if (transactionDTO.amount != null && senderWallet.id != null && receiverWallet.id != null) {

            // Calculate the new money
            senderWallet.amount -= transactionDTO.amount
            receiverWallet.amount += transactionDTO.amount

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
                receiverWalletId = receiverWallet.id
            )

            return transactionRepository.save(newTransaction).awaitSingleOrNull()?.toTransactionDTO()
        }

        throw ErrorResponse(HttpStatus.BAD_REQUEST, "Generic Error")
    }

    override suspend fun getTransactionsByPeriod(
        userInfoJWT: UserInfoJWT,
        walletId: ObjectId,
        start: Instant,
        end: Instant
    ): Flux<TransactionEntity?> {

        val wallet = walletRepository.findById(walletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Wallet not found")

        // Check that the login user is the owner of the wallet requested or is an admin (can see others wallet)
        if (userInfoJWT.username == wallet.customerUsername || userInfoJWT.isAdmin()) {

            val transactions = transactionRepository.findAllByTimeBetweenAndSenderWalletIdOrReceiverWalletId(
                start,
                end,
                walletId,
                walletId
            )
            return transactions
        }

        throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see these transactions")

    }

    override suspend fun getTransactionByIdAndWalletId(
        userInfoJWT: UserInfoJWT,
        transactionId: ObjectId,
        walletId: ObjectId
    ): TransactionDTO {
        val wallet = walletRepository.findById(walletId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Wallet not found")

        val transaction = transactionRepository.findById(transactionId.toString()).awaitSingleOrNull()
            ?: throw ErrorResponse(HttpStatus.NOT_FOUND, "Transaction not found")


        // Check that the login user is the owner of the wallet requested or is an admin (can see others wallet)
        if (userInfoJWT.username == wallet.customerUsername || userInfoJWT.isAdmin()) {

            // Check that the requested transaction belongs to the wallet or is an admin
            if (transaction.senderWalletId == wallet.id || transaction.receiverWalletId == wallet.id || userInfoJWT.isAdmin()) {
                return transaction.toTransactionDTO()
            }
        }

        throw ErrorResponse(HttpStatus.UNAUTHORIZED, "You have no permission to see this transaction")

    }

    /*override fun processPayment(order: Order): Mono<Order?>? {

     /*val payed = true
        if(payed){
            return Mono.fromCallable<Order> {
                sendMessage("warehouse-out-0", Event(Event.Type.CREDIT_RESERVED, order.orderId, order))
                order
            }.subscribeOn(publishEventScheduler)
        }
        return Mono.fromCallable<Order> {
            sendMessage("order-out-0", Event(Event.Type.CREDIT_UNAVAILABLE, order.orderId, order))
            order
        }.subscribeOn(publishEventScheduler)
        */
        return mono { Order() }

    }*/


    /* fun sendMessage(bindingName: String, event: Event<*, *>) {
         LOG.debug(
             "Sending a {} message to {}",
             event.eventType,
             bindingName
         )
         val message: Message<*> = MessageBuilder.withPayload<Any>(event)
             .setHeader("partitionKey", event.key)
             .build()
         streamBridge.send(bindingName, message)
     }*/


    companion object {
        private val LOG = LoggerFactory.getLogger(WalletServiceImpl::class.java)
    }
}