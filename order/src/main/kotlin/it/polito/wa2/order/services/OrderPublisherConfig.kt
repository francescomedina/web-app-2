package it.polito.wa2.order.services

import it.polito.wa2.api.event.OrderEvent
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.function.Supplier


/*@Component("warehouseEventProcessor")
@AllArgsConstructor
class WarehouseEventProcessor@Autowired constructor(warehouseService: WarehouseService) : Function<Event<Int?, Order?>, Event<Int?,Order?>> {
    private val warehouseService: WarehouseService

    override fun apply(event: Event<Int?, Order?>) {
        val order: Order = event.data!!

    }

    init {
        this.warehouseService = warehouseService
    }
}*/
/*
@Component("warehouseEventProcessor")
@AllArgsConstructor
class WarehouseEventProcessor @Autowired constructor(warehouseService: WarehouseService): Consumer<Event<Int?, Order?>> {
    private val warehouseService: WarehouseService

    override fun accept(event: Event<Int?, Order?>) {
        when (event.eventType) {
            Event.Type.ORDER_CREATED -> {
                val order: Order = event.data!!
                warehouseService.checkAvailability(order)!!.block()
            }
            Event.Type.CREDIT_RESERVED -> {
                val order: Order = event.data!!
                warehouseService.decrementQuantity(order)!!.block()
            }
            else -> {
                println("CIao")
            }
        }
    }

    init {
        this.warehouseService = warehouseService
    }
}*/
@EnableAutoConfiguration
@Component
class OrderPublisherConfig {
    @Bean
    fun orderSinks(): Sinks.Many<OrderEvent> {
        return Sinks.many().multicast().onBackpressureBuffer()
    }

    /*This will send message to kafka topic order-event based on configuration defined in application.yaml
    definitions will use method bean name order supplier*/
//    @Bean
//    fun orderSupplier(sinks: Sinks.Many<OrderEvent?>?): Supplier<Flux<OrderEvent>> {
//        return sinks::asFlux
//    }

    @Bean
    fun orderSupplier(sinks: Sinks.Many<OrderEvent?>?): () -> Flux<OrderEvent?> {
        return sinks!!::asFlux
    }
}