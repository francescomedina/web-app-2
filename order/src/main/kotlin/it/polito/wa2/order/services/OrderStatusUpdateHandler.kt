package it.polito.wa2.order.services

import it.polito.wa2.api.core.warehouse.Warehouse
import it.polito.wa2.api.event.Event
import it.polito.wa2.api.event.WarehouseEvent
import it.polito.wa2.order.persistence.OrderRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.function.Consumer

@EnableAutoConfiguration
@Component
class OrderStatusUpdateHandler @Autowired constructor(
    repository: OrderRepository?,
    publisher: OrderStatusPublisher?
){
    private val repository: OrderRepository?
    private val publisher: OrderStatusPublisher? = null

    @Transactional
    fun updateOrder(id: Int, consumer: Consumer<Warehouse?>) {
//        repository.findById(id).ifPresent(consumer.andThen(this::updateOrder))
        println("CIAO")
    }

//    private fun updateOrder(purchaseOrder: PurchaseOrder) {
//        val isPaymentComplete: Boolean = PaymentStatus.PAYMENT_COMPLETED.equals(purchaseOrder.getPaymentStatus())
//        val orderStatus: OrderStatus =
//            if (isPaymentComplete) OrderStatus.ORDER_COMPLETED else OrderStatus.ORDER_CANCELLED
//        purchaseOrder.setOrderStatus(orderStatus)
//        if (!isPaymentComplete) {
//            publisher!!.publishOrderEvent(convertEntityToDto(purchaseOrder), orderStatus)
//        }
//    }

//    fun convertEntityToDto(purchaseOrder: PurchaseOrder): OrderRequestDto {
//        val orderRequestDto = OrderRequestDto()
//        orderRequestDto.setOrderId(purchaseOrder.getId())
//        orderRequestDto.setUserId(purchaseOrder.getUserId())
//        orderRequestDto.setAmount(purchaseOrder.getPrice())
//        orderRequestDto.setProductId(purchaseOrder.getProductId())
//        return orderRequestDto
//    }

    init {
        this.repository = repository
    }
}