package it.polito.wa2.api.core.wallet

import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.event.OrderEvent
import it.polito.wa2.api.event.WalletEvent
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import reactor.core.publisher.Mono


interface WalletService {
    fun createWallet(body: Wallet?): Mono<Wallet?>?

    /**
     * Sample usage: "curl $HOST:$PORT/product/1".
     *
     * @param walletId Id of the product
     * @return the product, if found, else null
     */
    @GetMapping(value = ["/order/{walletId}"], produces = ["application/json"])
    fun getWallet(@PathVariable walletId: Int): Mono<Wallet?>?
    fun processPayment(orderEvent: OrderEvent): Mono<WalletEvent>
    fun deleteWallet(orderId: Int): Mono<Void?>?
}