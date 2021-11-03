package it.polito.wa2.warehouse.services

import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.core.warehouse.Warehouse
import it.polito.wa2.api.core.warehouse.WarehouseService
import it.polito.wa2.api.event.Event
import it.polito.wa2.api.event.OrderEvent
import it.polito.wa2.api.event.WarehouseEvent
import it.polito.wa2.util.http.ServiceUtil
import it.polito.wa2.warehouse.persistence.WarehouseRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler

@RestController
@EnableAutoConfiguration
class WarehouseServiceImpl @Autowired constructor(
    @Qualifier("publishEventScheduler") publishEventScheduler: Scheduler,
    repository: WarehouseRepository,
    mapper: WarehouseMapper,
    streamBridge: StreamBridge,
    serviceUtil: ServiceUtil
) : WarehouseService {
    private val serviceUtil: ServiceUtil
    private val repository: WarehouseRepository
    private val mapper: WarehouseMapper
    private val streamBridge: StreamBridge
    private val publishEventScheduler: Scheduler

    init {
        this.repository = repository
        this.mapper = mapper
        this.serviceUtil = serviceUtil
        this.streamBridge = streamBridge
        this.publishEventScheduler = publishEventScheduler
    }

    override fun checkAvailability(orderEvent: OrderEvent?): Mono<WarehouseEvent>{
        /// TODO: check availability
        val available = true
        val warehouse: Warehouse = Warehouse()
        return Mono.fromCallable {
            WarehouseEvent(WarehouseEvent.Type.QUANTITY_AVAILABLE, 123, warehouse)
        }
    }

    override fun decrementQuantity(order: Order?): Mono<Order?>? {
        /// TODO: decrement the product quantity
        val decremented = true
        if(decremented){
            return Mono.fromCallable<Order> {
                sendMessage("order-out-0", Event(Event.Type.QUANTITY_DECREASED, order!!.orderId, order))
                order
            }.subscribeOn(publishEventScheduler)
        }
        // product is no more available on the warehouse or service fails
        return Mono.fromCallable<Order> {
            sendMessage("wallet-out-0", Event(Event.Type.ROLLBACK_PAYMENT, order!!.orderId, order))
            order
        }.subscribeOn(publishEventScheduler)
    }

    override fun createWarehouse(body: Warehouse?): Mono<Warehouse?>? {
        return Mono.fromCallable<Warehouse> {
            if (body != null) {
                sendMessage("order-out-0", Event(Event.Type.QUANTITY_DECREASED, body.orderId, body))
            }
            body
        }.subscribeOn(publishEventScheduler)
//        return Mono.just(body!!)
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

    override fun getWarehouse(warehouseId: Int): Mono<Warehouse?>? {
        TODO("Not yet implemented")
    }

    override fun deleteWarehouse(orderId: Int): Mono<Void?>? {
        TODO("Not yet implemented")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(WarehouseServiceImpl::class.java)
    }
}