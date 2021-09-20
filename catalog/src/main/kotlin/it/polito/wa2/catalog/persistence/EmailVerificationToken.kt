package it.polito.wa2.catalog.persistence

import org.springframework.data.mongodb.core.mapping.Document
import java.util.Date
import java.sql.Timestamp

@Document("email-verification-token")
data class EmailVerificationToken(
    var expiryDate: Date,
    var token: String,
    var username: String,
)
