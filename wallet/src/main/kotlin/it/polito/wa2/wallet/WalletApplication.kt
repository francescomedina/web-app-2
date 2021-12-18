package it.polito.wa2.wallet

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.ComponentScan
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
@EnableEurekaClient
@ComponentScan("it.polito.wa2")
@RestController
class WalletApplication

fun main(args: Array<String>) {
    runApplication<WalletApplication>(*args)
}
