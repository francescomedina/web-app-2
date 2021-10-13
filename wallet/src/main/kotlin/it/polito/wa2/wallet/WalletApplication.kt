package it.polito.wa2.wallet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan("it.polito.wa2")
class WalletApplication

fun main(args: Array<String>) {
    runApplication<WalletApplication>(*args)
}
