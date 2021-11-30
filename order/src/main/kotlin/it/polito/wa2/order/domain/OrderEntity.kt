package it.polito.wa2.order.domain

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import javax.validation.constraints.Min


@Document(collection = "order")
data class OrderEntity (
    @Id
    var id: ObjectId? = ObjectId.get(),
    var status: String? = null,
    var buyer: String? = null,
)
