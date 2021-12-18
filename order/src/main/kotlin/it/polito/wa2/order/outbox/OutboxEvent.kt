package it.polito.wa2.order.outbox

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field
import java.time.Instant
import java.time.Instant.now


@Document(collection = "outbox-event")
data class OutboxEvent (
    @Id
    @Field(name = "message_id")
    var messageId: ObjectId? = ObjectId.get(),

    @Field(name = "channel")
    var channel: String,

    @Field(name = "message_key")
    var messageKey: String,

    @Field(name = "payload")
    var payload: String,

    @Field(name = "headers")
    var headers: String? = null,

    val timestamp: Instant = now(),

    val type: String,
)