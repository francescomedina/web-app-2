package it.polito.wa2.wallet.domain

import org.bson.types.ObjectId
import java.math.BigDecimal
import java.time.Instant
import javax.validation.constraints.Min

data class TransactionEntity(
   @Min(0) //Amount greater or equal to 0
    var amount: BigDecimal,

    var time: Instant = Instant.now(),

    var senderWalletId: ObjectId,

    var receiverWalletId: ObjectId,

    var reason : String = ""
)
