package it.polito.wa2.catalog.services

import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.api.core.order.Order
import it.polito.wa2.api.core.order.OrderService
import it.polito.wa2.api.exceptions.InvalidInputException
import it.polito.wa2.api.exceptions.NotFoundException
import it.polito.wa2.util.http.HttpErrorInfo
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.io.IOException
import java.util.logging.Level

@EnableAutoConfiguration
@Component
//TODO: Testare con branch di francesco
class CatalogIntegration @Autowired constructor(
    mapper: ObjectMapper,
    webClientBuilder: WebClient.Builder,
    streamBridge: StreamBridge
) : OrderService {
    private val webClient: WebClient
    private val mapper: ObjectMapper
    private val streamBridge: StreamBridge

    private val ORDER_SERVICE_URL = "http://order:8080"
    private val WALLET_SERVICE_URL = "http://wallet:8080"
    private val WAREHOUSE_SERVICE_URL = "http://warehouse:8080"

    init {
        this.webClient = webClientBuilder.build()
        this.mapper = mapper
        this.streamBridge = streamBridge
    }

    override fun createOrder(body: Order?): Mono<Order?>? {
        val url = "$ORDER_SERVICE_URL/orders/"
        return webClient
            .post()
            .uri(url)
            .body(Mono.just<Order>(body!!), Order::class.java)
            .retrieve()
            .bodyToMono(Order::class.java)
            .log(LOG.name, Level.FINE)
            .onErrorMap(
                WebClientResponseException::class.java
            ){ ex: WebClientResponseException -> handleException(ex) }
//        return webClient
//            .get()
//            .uri(url)
//            .retrieve()
//            .bodyToMono(Order::class.java)
//            .log(LOG.name, Level.FINE)
//            .onErrorMap(
//                WebClientResponseException::class.java
//            ){ ex: WebClientResponseException -> handleException(ex) }
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

    override fun updateStatus(order: Order, status: String) {
        TODO("Not yet implemented")
    }

    override fun deleteOrder(orderId: Int): Mono<Void?>? {
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

    companion object {
        private val LOG = LoggerFactory.getLogger(CatalogIntegration::class.java)
    }
}