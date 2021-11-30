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
    val id: ObjectId? = ObjectId.get(),

    var customerUsername: String = "",

    @Min(0) //Amount greater or equal to 0
    var amount: BigDecimal = BigDecimal(0)

)
