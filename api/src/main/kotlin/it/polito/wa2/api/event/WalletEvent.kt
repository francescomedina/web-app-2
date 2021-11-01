package it.polito.wa2.api.event

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer
import it.polito.wa2.api.core.wallet.Wallet
import lombok.AllArgsConstructor
import lombok.Getter
import lombok.NoArgsConstructor
import java.time.ZonedDateTime


@AllArgsConstructor
@NoArgsConstructor
@Getter
class WalletEvent(val eventType: Type?, val key: Int, val data: Wallet) {
    enum class Type {
        CREDIT_RESERVED,
        CREDIT_UNAVAILABLE,
    }

    @get:JsonSerialize(using = ZonedDateTimeSerializer::class)
    val eventCreatedAt: ZonedDateTime? = ZonedDateTime.now()
}