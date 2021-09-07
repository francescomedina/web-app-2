package it.polito.wa2.util.http

import org.springframework.http.HttpStatus
import java.time.ZonedDateTime


class HttpErrorInfo {
    private val timestamp: ZonedDateTime?
    private val path: String?
    private val httpStatus: HttpStatus?
    private val message: String?

    constructor() {
        timestamp = null
        httpStatus = null
        path = null
        message = null
    }

    constructor(httpStatus: HttpStatus?, path: String?, message: String?) {
        timestamp = ZonedDateTime.now()
        this.httpStatus = httpStatus
        this.path = path
        this.message = message
    }

    val status: Int
        get() = httpStatus!!.value()
    val error: String
        get() = httpStatus!!.reasonPhrase
}