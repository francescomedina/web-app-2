package it.polito.wa2.api.exceptions

import org.springframework.http.HttpStatus

data class AppRuntimeException(var errorMsg: String?, var httpStatus: HttpStatus,var ex: Throwable?)
    : RuntimeException(errorMsg, ex)