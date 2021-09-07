package it.polito.wa2.util.http

import it.polito.wa2.api.exceptions.BadRequestException
import it.polito.wa2.api.exceptions.InvalidInputException
import it.polito.wa2.api.exceptions.NotFoundException
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
internal class GlobalControllerExceptionHandler {
    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    @org.springframework.web.bind.annotation.ExceptionHandler(
        BadRequestException::class
    )
    @ResponseBody
    fun handleBadRequestExceptions(
        request: org.springframework.http.server.reactive.ServerHttpRequest, ex: BadRequestException
    ): HttpErrorInfo {
        return createHttpErrorInfo(org.springframework.http.HttpStatus.BAD_REQUEST, request, ex)
    }

    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
    @org.springframework.web.bind.annotation.ExceptionHandler(
        NotFoundException::class
    )
    @ResponseBody
    fun handleNotFoundExceptions(
        request: org.springframework.http.server.reactive.ServerHttpRequest, ex: NotFoundException
    ): HttpErrorInfo {
        return createHttpErrorInfo(org.springframework.http.HttpStatus.NOT_FOUND, request, ex)
    }

    @org.springframework.web.bind.annotation.ResponseStatus(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY)
    @org.springframework.web.bind.annotation.ExceptionHandler(
        InvalidInputException::class
    )
    @ResponseBody
    fun handleInvalidInputException(
        request: org.springframework.http.server.reactive.ServerHttpRequest, ex: InvalidInputException
    ): HttpErrorInfo {
        return createHttpErrorInfo(org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY, request, ex)
    }

    private fun createHttpErrorInfo(
        httpStatus: org.springframework.http.HttpStatus,
        request: org.springframework.http.server.reactive.ServerHttpRequest,
        ex: Exception
    ): HttpErrorInfo {
        val path: String = request.getPath().pathWithinApplication().value()
        val message = ex.message
        LOG.debug("Returning HTTP status: {} for path: {}, message: {}", httpStatus, path, message)
        return HttpErrorInfo(httpStatus, path, message)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(GlobalControllerExceptionHandler::class.java)
    }
}