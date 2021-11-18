package it.polito.wa2.order.services

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.core.order.OrderService
import it.polito.wa2.api.event.Event
import it.polito.wa2.api.exceptions.InvalidInputException
import it.polito.wa2.api.exceptions.NotFoundException
import it.polito.wa2.util.http.HttpErrorInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.http.HttpStatus
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import java.io.IOException
import java.util.logging.Level

@RestController
@EnableAutoConfiguration
@Component
class OrderIntegration @Autowired constructor(
    @Qualifier("publishEventScheduler") publishEventScheduler: Scheduler,
    mapper: ObjectMapper,
    webClientBuilder: WebClient.Builder,
    streamBridge: StreamBridge
) : OrderService {
    private val webClient: WebClient
    private val mapper: ObjectMapper
    private val streamBridge: StreamBridge
    private val publishEventScheduler: Scheduler

    private val ORDER_SERVICE_URL = "http://order"
    private val WALLET_SERVICE_URL = "http://wallet:8008"
    private val WAREHOUSE_SERVICE_URL = "http://warehouse:8008"

    init {
        this.webClient = webClientBuilder.build()
        this.mapper = mapper
        this.streamBridge = streamBridge
        this.publishEventScheduler = publishEventScheduler
    }

    override fun createOrder(body: Order?): Mono<Order?>? {
        return Mono.fromCallable<Order> {
            if (body != null) {
                sendMessage("order-out-0", Event(Event.Type.ORDER_CREATED, body.orderId, body))
            }
            body
        }.subscribeOn(publishEventScheduler)
    }

    override fun persistOrder(body: Order?): Order? {
        // TODO SALVO L'ORDINE SUL DB
        // PERCHE' HO LE CONDIZIONI NECESSARIE (CREDITO SUFFICIENTE E DISPONIBILITA' PRODOTTI) PER POTER PIAZZARE L'ORDINE
//        return Mono.fromCallable<Order> {
//            if (body != null) {
//                sendMessage("warehouse-out-0", Event(Event.Type.ORDER_CREATED, body.orderId, body))
//            }
//            body
//        }.subscribeOn(publishEventScheduler)
        return body
    }

    override fun getOrder(orderId: Int): Mono<Order?> {
        val url = "$ORDER_SERVICE_URL/orders/$orderId"
        return webClient
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(Order::class.java)
            .log(LOG.name, Level.FINE)
            .onErrorMap(
                WebClientResponseException::class.java
            ){ ex: WebClientResponseException -> handleException(ex) }
    }

    override fun getOrders(): Flux<Order?>? {
        TODO("Not yet implemented")
    }

    override fun updateStatus(order: Order, status: String) {
        /// TODO update order status ONCE PAYED
        // NOTIFY VIA EMAIL
        return
    }

    override fun deleteOrder(orderId: Int): Mono<Void?>? {
        return Mono.fromRunnable<Any> { sendMessage("order-out-0", Event(Event.Type.ORDER_CANCELLED, orderId, null)) }
            .subscribeOn(publishEventScheduler).then()
    }

    override fun putOrder(body: Order?): Mono<Order?>? {
        TODO("Not yet implemented")
    }


    fun handleException(ex: Throwable): Throwable? {
        if (ex !is WebClientResponseException) {
            LOG.warn("Got a unexpected error: {}, will rethrow it", ex.toString())
            return ex
        }
        val wcre = ex
        return when (wcre.statusCode) {
            HttpStatus.NOT_FOUND -> NotFoundException(getErrorMessage(wcre))
            HttpStatus.UNPROCESSABLE_ENTITY -> InvalidInputException(getErrorMessage(wcre))
            else -> {
                LOG.warn("Got an unexpected HTTP error: {}, will rethrow it", wcre.statusCode)
                LOG.warn("Error body: {}", wcre.responseBodyAsString)
                ex
            }
        }
    }

    fun getErrorMessage(ex: WebClientResponseException): String? {
        return try {
            mapper.readValue(ex.responseBodyAsString, HttpErrorInfo::class.java).getError()
        } catch (ioex: IOException) {
            ex.message
        }
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


    companion object {
        private val LOG = LoggerFactory.getLogger(OrderIntegration::class.java)
    }
}
