//package it.polito.wa2.wallet.services
//
//import it.polito.wa2.api.core.order.Order
//import it.polito.wa2.api.core.wallet.Wallet
//import it.polito.wa2.api.core.wallet.WalletService
//import it.polito.wa2.api.event.Event
//import it.polito.wa2.api.exceptions.EventProcessingException
//import org.slf4j.LoggerFactory
//import org.springframework.beans.factory.annotation.Autowired
//import org.springframework.boot.autoconfigure.EnableAutoConfiguration
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.stereotype.Component
//import java.util.function.Consumer
//
//@EnableAutoConfiguration
//@Component
//class MessageProcessorConfig @Autowired constructor(walletService: WalletService) {
//    private val walletService: WalletService
//
//    @Bean
//    fun messageProcessor(): Consumer<Event<Int?, Order?>> {
//        return Consumer<Event<Int?, Order?>> { event: Event<Int?, Order?> ->
//            LOG.info("WALLET SERVICE: Process message created at {}...", event.eventCreatedAt)
//            when (event.eventType) {
//                Event.Type.QUANTITY_AVAILABLE -> {
//                    val order: Order = event.data!!
//                    LOG.info("Process the payment for the order with ID: {}", order.orderId)
//                    walletService.processPayment(order)!!.block()
//                }
//                Event.Type.ROLLBACK_PAYMENT -> {
//                    val productId: Int = event.key!!
////                    LOG.info("Delete recommendations with ProductID: {}", productId)
////                    productService.deleteProduct(productId).block()
//                }
//                else -> {
//                    val errorMessage = "Incorrect event type: " + event.eventType
//                        .toString() + ", expected a CREATE or DELETE event"
//                    LOG.warn(errorMessage)
//                    throw EventProcessingException(errorMessage)
//                }
//            }
//            LOG.info("Message processing done!")
//        }
//    }
//
//    companion object {
//        val LOG = LoggerFactory.getLogger(MessageProcessorConfig::class.java)
//    }
//
//    init {
//        this.walletService = walletService
//    }
//}