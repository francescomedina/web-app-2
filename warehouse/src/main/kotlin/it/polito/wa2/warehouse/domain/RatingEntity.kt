package it.polito.wa2.warehouse.domain

import java.time.Instant

data class RatingEntity(
    val title: String,
    val body: String,

    val stars: Int,
    val createdDate: Instant = Instant.now(),
)
