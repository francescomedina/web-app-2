package it.polito.wa2.api.exceptions

import org.springframework.core.convert.ConversionFailedException
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException

@ControllerAdvice
class TranslatorHandler {


    @ExceptionHandler(ConversionFailedException::class)
    fun handleMethodArgumentTypeMismatchException(e: ConversionFailedException) {
        //TODO:
        /*val type = e. .requiredType
        val message: String = if (type!!.isEnum) {
            "The parameter ${e.name} must have a value among : ${type.enumConstants}"
        } else {
            "The parameter ${e.name} must be of type ${type.typeName}"
        }*/

        throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.toString())
    }


}