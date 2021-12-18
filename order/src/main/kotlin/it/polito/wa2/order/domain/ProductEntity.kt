package it.polito.wa2.order.domain

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import java.math.BigDecimal

data class ProductEntity (
    @Id
    var id: ObjectId? = ObjectId.get(),
    var amount: BigDecimal? = null,
    var price: BigDecimal? = null,
)
