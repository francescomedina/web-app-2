package it.polito.wa2.api.exceptions

import org.springframework.beans.TypeMismatchException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.util.stream.Collectors

@ControllerAdvice
class ValidationHandler {

    // This is the exception launched by @Valid annotation. It will catch and format the errors inside
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleException(e: WebExchangeBindException) {
        val errors = e.bindingResult
            .allErrors
            .stream()
            .map { obj: ObjectError -> obj.defaultMessage }
            .collect(Collectors.toList())
        // return ResponseEntity.badRequest().body(errors)
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, errors.toString())
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class, TypeMismatchException::class)
    fun handleException(e: TypeMismatchException): ResponseEntity<Map<String, String>>? {
        //val errorResponse: MutableMap<String, String> = HashMap()
        //errorResponse["message"] = e.localizedMessage
        //errorResponse["status"] = HttpStatus.BAD_REQUEST.toString()
        //return ResponseEntity(errorResponse, HttpStatus.BAD_REQUEST)
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.localizedMessage)
    }
}