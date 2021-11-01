package it.polito.wa2.wallet.services

import it.polito.wa2.api.composite.catalog.UserInfoJWT
import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.core.wallet.Wallet
import it.polito.wa2.api.event.Event
import it.polito.wa2.wallet.repositories.WalletRepository
import it.polito.wa2.util.http.ServiceUtil
import it.polito.wa2.wallet.controllers.ErrorResponse
import it.polito.wa2.wallet.domain.WalletEntity
import it.polito.wa2.wallet.dto.TransactionDTO
import it.polito.wa2.wallet.dto.WalletDTO
import it.polito.wa2.wallet.dto.toWalletDTO
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.http.HttpStatus
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.*

@Service
//@EnableAutoConfiguration
class WalletServiceImpl(
    //@Qualifier("publishEventScheduler") publishEventScheduler: Scheduler,
    val walletRepository: WalletRepository,
    //mapper: WalletMapper,
    //serviceUtil: ServiceUtil,
    //streamBridge: StreamBridge
) : WalletService {

    //private val serviceUtil: ServiceUtil
    //private val repository: WalletRepository
    //private val mapper: WalletMapper
    //private val streamBridge: StreamBridge
    //private val publishEventScheduler: Scheduler

    /*init {
        this.repository = repository
        this.mapper = mapper
        this.serviceUtil = serviceUtil
        this.streamBridge = streamBridge
       // this.publishEventScheduler = publishEventScheduler
    }*/

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

    override fun createTransaction(userId: Long, transactionDTO: TransactionDTO): TransactionDTO? {
        TODO("Not yet implemented")
    }

    override fun getAllWalletTransactions(walletId: Long): Iterable<TransactionDTO> {
        TODO("Not yet implemented")
    }

    override fun getTransactionsByPeriod(walletId: Long, start: Date, end: Date): Iterable<TransactionDTO> {
        TODO("Not yet implemented")
    }

    override fun getTransactionByIdAndWalletId(id: Long, wallet_id: Long): TransactionDTO? {
        TODO("Not yet implemented")
    }

    /*override fun processPayment(order: Order): Mono<Order?>? {
        /// TODO("Process payment")
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