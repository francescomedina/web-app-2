package it.polito.wa2.api.core.wallet

import it.polito.wa2.api.core.order.Order
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import reactor.core.publisher.Mono


/*interface WalletService {
    fun createWallet(body: WalletDTO?): Mono<Wallet?>?

    fun getWallet(@PathVariable walletId: Int): Mono<Wallet?>?
    fun processPayment(order: Order): Boolean
    fun deleteWallet(orderId: Int): Mono<Void?>?
}*/