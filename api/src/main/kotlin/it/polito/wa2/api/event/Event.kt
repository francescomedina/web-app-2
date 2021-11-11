package it.polito.wa2.api.event

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import java.time.ZonedDateTime


class Event<K, T> {
    enum class Type {
        ORDER_INIT,
        ORDER_CREATED,
        ORDER_CANCELLED,
        CREDIT_RESERVED,
        CREDIT_UNAVAILABLE,
        QUANTITY_AVAILABLE,
        QUANTITY_DECREASED,
        QUANTITY_UNAVAILABLE,
        ROLLBACK_PAYMENT,
        ROLLBACK_QUANTITY,
    }

    val eventType: Type?
    val key: K?
    val data: T?

    @get:JsonSerialize(using = ZonedDateTimeSerializer::class)
    val eventCreatedAt: ZonedDateTime?

    constructor() {
        eventType = null
        key = null
        data = null
        eventCreatedAt = null
    }

    constructor(eventType: Type?, key: K, data: T) {
        this.eventType = eventType
        this.key = key
        this.data = data
        eventCreatedAt = ZonedDateTime.now()
    }
}
