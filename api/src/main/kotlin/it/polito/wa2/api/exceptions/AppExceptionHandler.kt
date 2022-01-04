package it.polito.wa2.api.exceptions


import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import it.polito.wa2.api.ApiApplication
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.annotation.Order
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebExceptionHandler
import reactor.core.publisher.Mono


@Service
@Order(-2)
class AppsExceptionHandler(
    val objectMapper: ObjectMapper
) : WebExceptionHandler {

    private val LOG: Logger = LoggerFactory.getLogger(ApiApplication::class.java)

    override fun handle(exchange: ServerWebExchange, ex: Throwable): Mono<Void> {
        if (ex is AppRuntimeException) {
            val errorResponse = ErrorResponse(ex.httpStatus, ex.errorMsg.toString())
            LOG.error(ex.errorMsg)
            return try {
                exchange.response.statusCode = ex.httpStatus
                exchange.response.headers.contentType = MediaType.APPLICATION_JSON
                val db = DefaultDataBufferFactory().wrap(
                    objectMapper.writeValueAsBytes(errorResponse)
                )
                exchange.response.writeWith(Mono.just(db))
            } catch (e: JsonProcessingException) {
                e.printStackTrace()
                Mono.empty()
            }
        }
        return Mono.error(ex)
    }
}
