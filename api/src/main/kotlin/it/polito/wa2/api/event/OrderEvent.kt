package it.polito.wa2.api.event

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import it.polito.wa2.api.core.order.Order
import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import java.time.ZonedDateTime


@AllArgsConstructor
@NoArgsConstructor
@Getter
class OrderEvent(val eventType: Type?, val key: Int, val data: Order) {
    enum class Type {
        ORDER_CREATED,
        ORDER_CANCELLED,
    }

    @get:JsonSerialize(using = ZonedDateTimeSerializer::class)
    val eventCreatedAt: ZonedDateTime? = ZonedDateTime.now()
}