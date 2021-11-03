package it.polito.wa2.warehouse.services

import it.polito.wa2.api.core.warehouse.WarehouseService
import it.polito.wa2.api.event.OrderEvent
import it.polito.wa2.api.event.WarehouseEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.Function

@EnableAutoConfiguration
@Component
class WarehouseConsumerConfig @Autowired constructor(warehouseService: WarehouseService) {
    private val warehouseService: WarehouseService

    /* This method act as producer and consumer. Which consumer OrderEvent from kafka topic
        and produce PaymentEvent in Kafka Topic. Configuration will be defined in application.yaml based on method name.
    * */
    @Bean
    fun warehouseProcessor(): Function<Flux<OrderEvent>, Flux<WarehouseEvent>> {
        return Function<Flux<OrderEvent>, Flux<WarehouseEvent>> { orderEventFlux ->
            orderEventFlux.flatMap { orderEvent: OrderEvent ->
                processWarehouse(
                    orderEvent
                )
            }
        }
    }

    private fun processWarehouse(orderEvent: OrderEvent): Mono<WarehouseEvent> {
        // get the user id
        // check the balance availability
        // if balance sufficient -> Payment completed and deduct amount from DB
        // if payment not sufficient -> cancel order event and update the amount in DB
        return if (OrderEvent.Type.ORDER_CREATED === orderEvent.eventType) {
            warehouseService.checkAvailability(orderEvent)
        } else {
            warehouseService.checkAvailability(orderEvent)
        }
    }

    init {
        this.warehouseService = warehouseService
    }
}