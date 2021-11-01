package it.polito.wa2.api.event

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import it.polito.wa2.api.core.warehouse.Warehouse
import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import java.time.ZonedDateTime


@AllArgsConstructor
@NoArgsConstructor
@Getter
class WarehouseEvent(val eventType: Type?, val key: Int, val data: Warehouse) {
    enum class Type {
        QUANTITY_AVAILABLE,
        QUANTITY_DECREASED,
        QUANTITY_UNAVAILABLE,
    }

    @get:JsonSerialize(using = ZonedDateTimeSerializer::class)
    val eventCreatedAt: ZonedDateTime? = ZonedDateTime.now()
}