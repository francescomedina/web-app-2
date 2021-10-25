package it.polito.wa2.catalog.DTO

import javax.validation.constraints.NotNull

// This is the representation of what the user will send during the login phase
data class Login(
    @field:NotNull(message = "You must provide an username")
    val username: String,
    val password: String)

data class Profile(val username: String)

data class EnableUser(
    val username: String,
    val enable: Boolean
)

data class ChangePasswordBody(
    val oldPassword: String,
    val password: String,
    val confirmPassword: String
)
