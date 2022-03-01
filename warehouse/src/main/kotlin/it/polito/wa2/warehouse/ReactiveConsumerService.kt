package it.polito.wa2.warehouse

import com.fasterxml.jackson.annotation.JsonProperty
import it.polito.wa2.util.gson.GsonUtils.Companion.gson
import it.polito.wa2.warehouse.domain.ProductAvailabilityEntity
import it.polito.wa2.warehouse.outbox.OutboxEventPublisher
import it.polito.wa2.warehouse.repository.ProductAvailabilityRepository
import it.polito.wa2.warehouse.services.MailService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.header.internals.RecordHeader
import org.bson.types.ObjectId
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate
import org.springframework.messaging.support.GenericMessage
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.Assert
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.toMono
import java.math.BigDecimal
import java.util.*

data class ProductEntity(
    @JsonProperty("id")
    var id: ObjectId,
    @JsonProperty("quantity")
    var quantity: Int,
    @JsonProperty("price")
    var price: BigDecimal
)

data class DeliveryEntity(
    @JsonProperty("shippingAddress")
    var shippingAddress: String?,
    @JsonProperty("warehouseId")
    var warehouseId: ObjectId,
    @JsonProperty("products")
    var products: List<ProductEntity> = emptyList(),
)

data class OrderEntity(
    @JsonProperty("id")
    var id: ObjectId,
    @JsonProperty("status")
    var status: String? = null,
    @JsonProperty("buyer")
    var buyer: String? = null,
    @JsonProperty("products")
    var products: List<ProductEntity> = emptyList(),
    @JsonProperty("delivery")
    var delivery: List<DeliveryEntity>? = emptyList(),
)


@Service
class ReactiveConsumerService(
    val reactiveKafkaConsumerTemplate: ReactiveKafkaConsumerTemplate<String, String>,
    val reactiveProducerService: ReactiveProducerService,
    val productAvailabilityRepository: ProductAvailabilityRepository,
    val eventPublisher: OutboxEventPublisher,
    val mailService: MailService,
) : CommandLineRunner {

    private val adminEmail = "pacimedina@gmail.com"
    private val log = LoggerFactory.getLogger(ReactiveConsumerService::class.java)

    fun checkAvailability(order: OrderEntity): Mono<MutableList<ProductAvailabilityEntity?>> {
        return Flux.fromIterable(order.products)
            .flatMap { productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity) }
            .switchIfEmpty(Mono.error(RuntimeException("Cannot find the product")))
            .onErrorResume {
                log.error("Error during check availability; reason: ${it.message}")
                reactiveProducerService.send(
                    ProducerRecord("warehouse.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE_NOT_PURCHASED".toByteArray())))
                )
                throw RuntimeException("CIAONE 1")
            }
            .collectList()
            .doOnNext {
                log.info("SAGA-WAREHOUSE: Sending QUANTITY_AVAILABLE to warehouse.topic ")

                reactiveProducerService.send(
                    ProducerRecord(
                        "warehouse.topic",
                        null,
                        order.id.toString(),
                        gson.toJson(order),
                        listOf(RecordHeader("type", "QUANTITY_AVAILABLE".toByteArray()))
                    )
                )
            }
    }

    @Transactional
    fun checkProductAvailability(order: OrderEntity) {
        try {
            checkAvailability(order).subscribe()
        }catch (r: RuntimeException){
            throw RuntimeException("ALTROOOOO")
        }
//        return reactiveProducerService.send(
//            ProducerRecord(
//                "warehouse.topic",
//                null,
//                order.id.toString(),
//                gson.toJson(order),
//                listOf(RecordHeader("type", "QUANTITY_AVAILABLE".toByteArray()))
//            )
//        )
//        Mono.just(order)
//            .flatMap {
//                Flux.fromIterable(order.products)
//                    .flatMap { productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity) }
//                    .switchIfEmpty(Mono.error(RuntimeException("Can not find the product")))
//                    .onErrorResume {
//                        log.info("CIAONE 1")
//                        reactiveProducerService.send(
//                            ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
//                        )
//                        throw RuntimeException("CIAONE 1")
//                    }
//                    .flatMap { Mono.just(order) }
//                Mono.just(order)
//            }
//            .onErrorResume {
//                log.info("CIAONE X")
//                reactiveProducerService.send(
//                    ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
//                )
//                throw RuntimeException("CIAONE X ")
//            }
//            .flatMap {
//                Flux.fromIterable(order.products)
//                    .flatMap { productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity) }
//                    .switchIfEmpty(Mono.error(RuntimeException("Can not find item.")))
//                    .onErrorResume {
//                        log.info("CIAONE 2")
//                        reactiveProducerService.send(
//                            ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
//                        )
//                        throw RuntimeException("CIAONE 2")
//                    }
//                Mono.just(order)
//            }
//            .onErrorResume {
//                log.info("CIAONE 3")
//                reactiveProducerService.send(
//                    ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
//                )
//                throw RuntimeException("CIAONE 3 ")
//            }
//            .subscribe()
//        return reactiveProducerService.send(
//            ProducerRecord(
//                "warehouse.topic",
//                null,
//                order.id.toString(),
//                gson.toJson(order),
//                listOf(RecordHeader("type", "QUANTITY_AVAILABLE".toByteArray()))
//            )
//        )
//            .doOnNext {
//                checkAvailability(order).subscribe()
//            }
//            .onErrorResume {
//                log.info("CIAONE 2")
//                reactiveProducerService.send(
//                    ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
//                )
//                throw RuntimeException("CIAONE 2")
//            }

//            checkExistence(order)
//                .onErrorResume {
//                    log.info("CIAONE")
//                    throw RuntimeException("CIAONE")
//                }
//            checkAvailability(order)
//                .onErrorResume {
//                    log.info("CIAONE 2")
//                    throw RuntimeException("CIAONE 2")
//                }.subscribe()
//            reactiveProducerService.send(
//                ProducerRecord(
//                    "warehouse.topic",
//                    null,
//                    order.id.toString(),
//                    gson.toJson(order),
//                    listOf(RecordHeader("type", "QUANTITY_AVAILABLE".toByteArray()))
//                )
//            )

/*        return Mono.just(order.products)
            .flatMapMany { products ->
                Flux.fromIterable(products)
                    .doOnNext {
                        productAvailabilityRepository.findOneByProductId(it.id)
                            .doOnNext { p -> Assert.isTrue(p!=null,"Product not found") }
                            .doOnError {
                                reactiveProducerService.send(
                                    ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
                                )
                                throw RuntimeException(it)
                            }
                            .onErrorResume {
                                reactiveProducerService.send(
                                    ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
                                )
                                throw RuntimeException(it)
                            }
                            .subscribe()
                    }
                    .doOnNext {
                        productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
                            .doOnNext { p -> Assert.isTrue(p!=null,"Product no more available") }
                            .subscribe()
                    }
                    .doOnError {
                        reactiveProducerService.send(
                            ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
                        )
                        throw RuntimeException(it)
                    }
            }
            .onErrorResume {
                reactiveProducerService.send(
                    ProducerRecord("order.topic", null, order.id.toString(), gson.toJson(order),listOf(RecordHeader("type", "QUANTITY_UNAVAILABLE".toByteArray())))
                )
                throw RuntimeException(it)
            }
            .doOnComplete {
                reactiveProducerService.send(
                    ProducerRecord(
                        "warehouse.topic",
                        null,
                        order.id.toString(),
                        gson.toJson(order),
                        listOf(RecordHeader("type", "QUANTITY_AVAILABLE".toByteArray()))
                    )
                )
            }*/
    }

    private fun warehouseConsumer(): Flux<ConsumerRecord<String, String>> {
        return reactiveKafkaConsumerTemplate
            .receiveAutoAck()
            .doOnNext { consumerRecord: ConsumerRecord<String, String> ->
                log.info(
                    "received key={}, value={} from topic={}, offset={}, headers={}",
                    consumerRecord.key(),
                    consumerRecord.value(),
                    consumerRecord.topic(),
                    consumerRecord.offset(),
                    consumerRecord.headers()
                )
            }
            .doOnNext {
                //val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                //val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)
                when (String(it.headers().reduce { _, header -> if(header.key() == "type") header else null}?.value() as ByteArray)){
                    "ORDER_CREATED" -> {
                        log.info("SAGA-WAREHOUSE: Reading ORDER_CREATED from order.topic")

                        val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                        val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)

                        checkProductAvailability(order)
                    }
                    "ORDER_CANCELED" -> {
                        log.info("SAGA-WAREHOUSE: Reading ORDER_CANCELED from order.topic")

                        val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                        val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)

                        incrementQuantity(order).subscribe()
                    }
                    "TRANSACTION_SUCCESS" -> {
                        log.info("SAGA-WAREHOUSE: Reading TRANSACTION_SUCCESS from wallet.topic")

                        val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                        val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)

                        decrementQuantity(order).subscribe()
                    }
                    "REFUND_TRANSACTION_ERROR" -> {
                        log.info("SAGA-WAREHOUSE: Reading REFUND_TRANSACTION_ERROR from wallet.topic")

                        val genericMessage = gson.fromJson(it.value(), GenericMessage::class.java)
                        val order = gson.fromJson(genericMessage.payload.toString(),OrderEntity::class.java)

                        incrementQuantity(order, true).subscribe()
                    }
                    else -> {
                        log.error(
                            "SAGA-WAREHOUSE: Unknown message type from order.topic ${
                                String(
                                    it.headers().reduce { _, header -> if (header.key() == "type") header else null }
                                        ?.value() as ByteArray
                                )
                            }"
                        )
                    }
                }
            }
            .doOnError { throwable: Throwable ->
                log.error("SAGA-WAREHOUSE: ERROR Something bad happened while consuming : {}", throwable.message)
            }
    }

    @Transactional
    fun decrementQuantity(order: OrderEntity): Flux<ProductAvailabilityEntity> {
        val deliveryMap = hashMapOf<ObjectId, MutableList<ProductEntity>>()
        return Flux.fromIterable(order.products)
                .flatMap {
                    productAvailabilityRepository.findOneByProductIdAndQuantityGreaterThanEqual(it.id,it.quantity)
                        .doOnNext { p ->
                            Assert.isTrue(p!=null, "Product is no more available")
                            p!!.quantity -= it.quantity
                            if(deliveryMap[p!!.warehouseId].isNullOrEmpty()){
                                deliveryMap[p.warehouseId] = mutableListOf(it)
                            }else{
                                deliveryMap[p.warehouseId]?.add(it)
                            }
                        }
                }
                .flatMap { productAvailabilityRepository.save(it!!) }
                .doOnNext {
                    if(it.quantity < it.min_quantity){
                        mailService.sendMessage(
                            adminEmail,
                            "ALERT - Product ${it.productId}",
                            "The product ${it.productId} quantity is below the minimum threshold (${it.min_quantity}).<b> WarehouseID: ${it.warehouseId}"
                        )
                    }
                }
                .doOnError { throw RuntimeException("Error on productAvailability") }
                .doOnComplete { // Si applica alla fine di tutti i flussi, in questo caso dopo aver iterato tutti i prodotti
                    val tmp = mutableListOf<DeliveryEntity>()
                    deliveryMap.forEach {
                        tmp.add(DeliveryEntity(
                            "Via Alessandro Volta 123",
                            it.key,
                            it.value.toList()
                        ))
                    }
                    order.delivery = tmp.toList()

                    log.info("SAGA-WAREHOUSE: Sending QUANTITY_DECREMENTED to warehouse.topic")

                    eventPublisher.publish(
                        "warehouse.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "QUANTITY_DECREMENTED"
                    ).subscribe{
                        log.info("successfully consumed DECREMENT QUANTITY {} ", it)
                    }
                }
                .doOnError {
                    log.info("SAGA-WAREHOUSE: Sending QUANTITY_UNAVAILABLE to wallet.topic")

                    eventPublisher.publish(
                        "wallet.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "QUANTITY_UNAVAILABLE"
                    ).subscribe {
                        throw RuntimeException("Error during updating products")
                    }
                }
    }

    @Transactional
    fun incrementQuantity(order: OrderEntity, dueToError: Boolean = false): Flux<DeliveryEntity> {
        return Flux.fromIterable(order.delivery!!)
            .doOnNext {
                Flux.fromIterable(it.products)
                    .doOnNext { p ->
                        productAvailabilityRepository.findOneByWarehouseIdAndProductId(it!!.warehouseId,p.id)
                            .doOnNext { pa ->
                                pa!!.quantity += p.quantity
                                productAvailabilityRepository.save(pa!!).subscribe()
                            }.subscribe()
                    }
                    .subscribe()
            }
            .doOnError { throw RuntimeException("Error on productAvailability") }
            .doOnComplete {
                if(!dueToError){
                    log.info("SAGA-WAREHOUSE: Sending WAREHOUSE_PRODUCTS_RETURNED to warehouse.topic")

                    eventPublisher.publish(
                        "warehouse.topic",
                        order.id.toString(),
                        gson.toJson(order),
                        "WAREHOUSE_PRODUCTS_RETURNED"
                    ).subscribe{
                        log.info("successfully consumed INCREMENT QUANTITY {} ", it)
                    }
                }
            }
            .doOnError {
                log.info("SAGA-WAREHOUSE: Sending WAREHOUSE_PRODUCTS_RETURNING_ERROR to warehouse.topic")

                eventPublisher.publish(
                    "warehouse.topic",
                    order.id.toString(),
                    gson.toJson(order),
                    "WAREHOUSE_PRODUCTS_RETURNING_ERROR"
                ).subscribe()
            }
    }

    override fun run(vararg args: String) {
        warehouseConsumer().subscribe()
    }
}