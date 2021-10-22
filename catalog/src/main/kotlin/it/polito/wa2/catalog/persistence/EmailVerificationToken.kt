package it.polito.wa2.catalog.persistence

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import java.sql.Timestamp

@Document("email-verification-token")
data class EmailVerificationToken(
    @Id
    val id: ObjectId = ObjectId.get(),
    var expiryDate: Date,
    var token: String,
    var username: String,
)
