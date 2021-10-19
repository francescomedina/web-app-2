package it.polito.wa2.wallet.services

import it.polito.wa2.api.core.wallet.Wallet
import it.polito.wa2.api.core.wallet.WalletService
import it.polito.wa2.wallet.persistence.WalletRepository
import it.polito.wa2.util.http.ServiceUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
@EnableAutoConfiguration
class WalletServiceImpl @Autowired constructor(
    repository: WalletRepository,
    mapper: WalletMapper,
    serviceUtil: ServiceUtil
) : WalletService {
    private val serviceUtil: ServiceUtil
    private val repository: WalletRepository
    private val mapper: WalletMapper

//    override fun deleteOrder(orderId: Int): Mono<Void?>? {
//        if (orderId < 1) {
//            throw InvalidInputException("Invalid orderId: $orderId")
//        }
//        LOG.debug("deleteProduct: tries to delete an entity with orderId: {}", orderId)
//        return repository.findByOrderId(orderId)
//            .log(LOG.name, Level.FINE)
//            .mapNotNull { e -> e?.let { repository.delete(it) } }
//            .flatMap { e -> e }
//    }
//
//    private fun setServiceAddress(e: Order): Order {
//        e.serviceAddress = serviceUtil.serviceAddress.toString()
//        return e
//    }
//
//    companion object {
//        private val LOG = LoggerFactory.getLogger(WalletServiceImpl::class.java)
//    }
//
    init {
        this.repository = repository
        this.mapper = mapper
        this.serviceUtil = serviceUtil
    }
//
////    override fun createOrder(body: Order): Mono<Order?>? {
////        if (body != null) {
////            if (body.orderId < 1) {
////                throw InvalidInputException("Invalid orderId: " + body.orderId)
////            }
////        }
////        val entity: OrderEntity = mapper.apiToEntity(body)
////
////        if (body != null) {
////            return repository.save(entity)
////                .log(LOG.name, Level.FINE)
////                .onErrorMap(DuplicateKeyException::class.java) { ex -> InvalidInputException("Duplicate key, Product Id: " + body.orderId)
////                }
////                .mapNotNull { e -> mapper.entityToApi(e) }
////        }
////        return null
////    }
//
//    override fun createOrder(body: Order?): Mono<Order?>? {
//        if (body != null) {
//            if (body.orderId < 1) {
//                throw InvalidInputException("Invalid orderId: " + body.orderId)
//            }
//        }
//        val entity: OrderEntity = body?.let { mapper.apiToEntity(it) }!!
//
//        if (body != null) {
//            return repository.save(entity)
//                .log(LOG.name, Level.FINE)
//                .onErrorMap(DuplicateKeyException::class.java) { ex -> InvalidInputException("Duplicate key, Product Id: " + body.orderId)
//                }
//                .mapNotNull { e -> mapper.entityToApi(e) }
//        }
//        return null
//    }
//
//    override fun getOrder(orderId: Int): Mono<Order?>? {
//        if (orderId < 1) {
//            throw InvalidInputException("Invalid orderId: $orderId")
//        }
//        LOG.info("Will get product info for id={}", orderId)
//
//        return repository.findByOrderId(orderId)
//            .switchIfEmpty(Mono.error(NotFoundException("No product found for orderId: $orderId")))
//            .log(LOG.name, Level.FINE)
//            .mapNotNull { e -> e?.let { mapper.entityToApi(it) } }
//            .mapNotNull { e -> e?.let { setServiceAddress(it) } }
//    }

    override fun createWallet(body: Wallet?): Mono<Wallet?>? {
        TODO("Not yet implemented")
    }

    override fun getWallet(walletId: Int): Mono<Wallet?>? {
        TODO("Not yet implemented")
    }

    override fun deleteWallet(orderId: Int): Mono<Void?>? {
        TODO("Not yet implemented")
    }
}