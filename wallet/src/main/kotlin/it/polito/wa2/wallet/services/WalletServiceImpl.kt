package it.polito.wa2.wallet.services

import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.core.wallet.Wallet
import it.polito.wa2.api.core.wallet.WalletService
import it.polito.wa2.api.core.warehouse.WarehouseService
import it.polito.wa2.api.event.Event
import it.polito.wa2.wallet.persistence.WalletRepository
import it.polito.wa2.util.http.ServiceUtil
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import reactor.core.scheduler.Scheduler

@RestController
@EnableAutoConfiguration
class WalletServiceImpl @Autowired constructor(
    @Qualifier("publishEventScheduler") publishEventScheduler: Scheduler,
    repository: WalletRepository,
    mapper: WalletMapper,
    serviceUtil: ServiceUtil,
    streamBridge: StreamBridge
) : WalletService {
    private val serviceUtil: ServiceUtil
    private val repository: WalletRepository
    private val mapper: WalletMapper
    private val streamBridge: StreamBridge
    private val publishEventScheduler: Scheduler

    init {
        this.repository = repository
        this.mapper = mapper
        this.serviceUtil = serviceUtil
        this.streamBridge = streamBridge
        this.publishEventScheduler = publishEventScheduler
    }

    override fun createWallet(body: Wallet?): Mono<Wallet?>? {
//        val wallet = Wallet()
        return Mono.just(body!!)
    }

    override fun getWallet(walletId: Int): Mono<Wallet?>? {
        TODO("Not yet implemented")
    }

    override fun processPayment(order: Order): Mono<Order?>? {
        /// TODO("Process payment")
        val payed = true
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
    }

    override fun deleteWallet(orderId: Int): Mono<Void?>? {
        TODO("Not yet implemented")
    }

    fun sendMessage(bindingName: String, event: Event<*, *>) {
        LOG.debug(
            "Sending a {} message to {}",
            event.eventType,
            bindingName
        )
        val message: Message<*> = MessageBuilder.withPayload<Any>(event)
            .setHeader("partitionKey", event.key)
            .build()
        streamBridge.send(bindingName, message)
    }


    companion object {
        private val LOG = LoggerFactory.getLogger(WalletServiceImpl::class.java)
    }
}