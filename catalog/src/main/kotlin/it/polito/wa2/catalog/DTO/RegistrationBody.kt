package it.polito.wa2.catalog.DTO

import javax.validation.constraints.Email
import javax.validation.constraints.NotNull

data class RegistrationBody(
    @field:Email(message = "Email is not valid")
    val email: String,

    val name: String,
    val surname: String,
    val address: String,

    @field:NotNull(message = "You must provide an username")
    val username: String,
    val password: String,
    val confirmPassword: String,
)