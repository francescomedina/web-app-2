package it.polito.wa2.api.event

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import java.time.ZonedDateTime


class Event<K, T> {
    enum class Type {
        CREATE, DELETE
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
