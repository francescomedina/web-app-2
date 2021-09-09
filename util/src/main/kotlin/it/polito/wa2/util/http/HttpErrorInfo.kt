package it.polito.wa2.util.http

import org.springframework.http.HttpStatus
import java.time.ZonedDateTime


data class HttpErrorInfo(
    private val httpStatus: HttpStatus? = null,
    val path: String? = null,
    val message: String? = null,
    val timestamp: ZonedDateTime = ZonedDateTime.now(),
){

    fun getError(): String? {
        return httpStatus!!.reasonPhrase
    }



}