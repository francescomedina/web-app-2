package it.polito.wa2.wallet.services

import it.polito.wa2.api.core.wallet.WalletService
import it.polito.wa2.api.event.Event
import it.polito.wa2.api.event.OrderEvent
import it.polito.wa2.api.event.WalletEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Function


/*@Component("walletEventProcessor")
@AllArgsConstructor
class WalletEventProcessor @Autowired constructor(walletService: WalletService): Consumer<Event<Int?, Order?>> {
    private val walletService: WalletService

    override fun accept(event: Event<Int?, Order?>) {
        when (event.eventType) {
            Event.Type.QUANTITY_AVAILABLE -> {
                val order: Order = event.data!!
                walletService.processPayment(order)!!.block()
            }
            Event.Type.ORDER_CREATED -> {
                val order: Order = event.data!!
                walletService.processPayment(order)!!.block()
            }
            else -> {
                println("asd")
            }
        }
    }

    init {
        this.walletService = walletService
    }
}*/
@EnableAutoConfiguration
@Component
class WalletConsumerConfig @Autowired constructor(walletService: WalletService) {
    private val walletService: WalletService

    /* This method act as producer and consumer. Which consumer OrderEvent from kafka topic
        and produce PaymentEvent in Kafka Topic. Configuration will be defined in application.yaml based on method name.
    * */
    @Bean
    fun paymentProcessor(): Function<Flux<OrderEvent>, Flux<WalletEvent>> {
        return Function<Flux<OrderEvent>, Flux<WalletEvent>> { orderEventFlux ->
            orderEventFlux.flatMap { orderEvent: OrderEvent ->
                processPayment(
                    orderEvent
                )
            }
        }
    }

    private fun processPayment(orderEvent: OrderEvent): Mono<WalletEvent> {
        // get the user id
        // check the balance availability
        // if balance sufficient -> Payment completed and deduct amount from DB
        // if payment not sufficient -> cancel order event and update the amount in DB
        return if (OrderEvent.Type.ORDER_CREATED == orderEvent.eventType) {
            walletService.processPayment(orderEvent)
        } else {
            walletService.processPayment(orderEvent)
        }
    }

    init {
        this.walletService = walletService
    }
}