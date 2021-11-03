package it.polito.wa2.order.services

import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.core.warehouse.WarehouseService
import it.polito.wa2.api.event.OrderEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import reactor.core.publisher.Sinks


@Service
class OrderStatusPublisher @Autowired constructor(orderSinks: Sinks.Many<OrderEvent>?) {
    private val orderSinks: Sinks.Many<OrderEvent>?
//    @Autowired
//    private val orderSinks: Sinks.Many<OrderEvent>? = null

    /*This is springboot webflux : reactor programming*/
    fun publishOrderEvent(order: Order) {
        val orderEvent = OrderEvent(OrderEvent.Type.ORDER_CREATED, order.orderId, order)
        orderSinks!!.tryEmitNext(orderEvent)
    }

    init {
        this.orderSinks = orderSinks
    }
}