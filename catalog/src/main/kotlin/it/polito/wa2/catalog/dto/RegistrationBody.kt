package it.polito.wa2.catalog.dto

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

data class RegistrationBody(
    @field:Email(message = "Email is not valid")
    val email: String,
    //TODO: Validate i DTO
    val name: String = "",
    val surname: String = "",
    val address: String = "",

    @field:NotBlank(message = "Missing required body parameter: username")
    val username: String = "",
    @field:NotBlank(message = "Missing required body parameter: password")
    val password: String = "",
    @field:NotBlank(message = "Missing required body parameter: confirmPassword")
    val confirmPassword: String = "",
)