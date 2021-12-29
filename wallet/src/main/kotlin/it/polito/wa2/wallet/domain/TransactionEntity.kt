package it.polito.wa2.wallet.domain

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.Instant
import javax.validation.constraints.Min
import javax.validation.constraints.NotBlank

@Document(collection = "transactions")
data class TransactionEntity(
    @Id
    val id: ObjectId = ObjectId.get(),

    @Min(0) //Amount greater or equal to 0
    var amount: BigDecimal,

    var time: Instant = Instant.now(),

    var senderWalletId: ObjectId,

    var receiverWalletId: ObjectId,

    var reason : String = ""
)
