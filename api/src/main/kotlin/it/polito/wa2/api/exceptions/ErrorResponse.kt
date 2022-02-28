package it.polito.wa2.api.exceptions

import org.springframework.http.HttpStatus

// Exception class used for send to the controller the correct message and status to generate an appropriate http message
data class ErrorResponse(val status: HttpStatus, val errorMessage: String) : Throwable(message = errorMessage)
