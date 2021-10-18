package it.polito.wa2.catalog.controller

import it.polito.wa2.catalog.DTO.*
import it.polito.wa2.catalog.persistence.UserEntity
import it.polito.wa2.catalog.persistence.UserRepository
import it.polito.wa2.catalog.security.JwtUtils
import it.polito.wa2.catalog.security.Rolename
import it.polito.wa2.catalog.services.ErrorResponse
import it.polito.wa2.catalog.services.UserDetailsServiceImpl
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.function.Function
import java.util.stream.Collectors
import javax.validation.Valid
import javax.validation.constraints.NotNull


@RestController
@RequestMapping("/auth")
class UserController(
    private val jwtUtils: JwtUtils,
    private val encoder: PasswordEncoder,
    private val users: UserDetailsServiceImpl,

    private val userDetailsServiceImpl: UserDetailsServiceImpl,
    private val userRepository: UserRepository

) {

    /**
     * Controller that handle the registration process
     * It will check if the password and confirmPassword are the same and then delegate the creation to the service above
     * @return the user created or a bad request if some errors occurred
     */
    @PostMapping("/register")
    suspend fun register(@RequestBody @Valid data: Mono<RegistrationBody>): ResponseEntity<UserDetailsDTO> {

        data.awaitSingleOrNull()?.let { it ->
            if (it.password == it.confirmPassword) {

                // Mapping the information inside RegistrationBody with a UserDetailsDTO format
                val userDTO = UserDetailsDTO(
                    _username = it.username,
                    _password = encoder.encode(it.password),
                    _email = it.email,
                    _roles = setOf(Rolename.CUSTOMER),
                    isEnable = false
                )

                // Talking to the service above to create the user
                try {
                    val createdUser = userDetailsServiceImpl.createCustomerUser(userDTO)

                    // Return a 201 with inside the user created
                    return ResponseEntity.status(HttpStatus.CREATED).body(createdUser)

                } catch (error: ErrorResponse) {
                    throw ResponseStatusException(error.status, error.errorMessage)
                }

            }
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The confirmation password and the password don't match"
            )
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }


    /**
     * Controller that handle the update password procedure
     * @param principal : user principal to get username
     * @param data : with the old password and the new password
     */
    @PostMapping("/updatePassword")
    suspend fun updatePassword(
        @AuthenticationPrincipal principal: Principal,
        @RequestBody @Valid data: Mono<ChangePasswordBody>
    ): ResponseEntity<String> {

        data.awaitSingleOrNull()?.let { body: ChangePasswordBody ->

            if (body.password != body.confirmPassword) {
                throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The confirmation password and the password don't match"
                )
            }

            // Now that the two new passwords are equal, we will check if the old password is correct
            try {
                // Take user info from DB
                val userInfo = userDetailsServiceImpl.getUserByUsername(principal.name)

                userInfo?.let { user ->

                    if (encoder.matches(body.oldPassword, user.password)) {

                        // The old password is the same we have inside the DB, so we can change it with the new one
                        userDetailsServiceImpl.updatePassword(
                            username = principal.name,
                            password = encoder.encode(body.password)
                        )

                        // Return a 201 with inside the user created
                        return ResponseEntity.status(HttpStatus.OK).body("Password updated correctly")

                    }
                    throw ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "The old password is not correct"
                    )
                }

            } catch (error: ErrorResponse) {
                // Some errors occurred, we return the errorMessage formatted inside updatePassword method
                throw ResponseStatusException(error.status, error.errorMessage)
            }
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST)
    }


    @GetMapping("/registrationConfirm")
    suspend fun confirmRegistration(@RequestParam("token") token: String): ResponseEntity<String> {

        try {
            val tokenInfo = userDetailsServiceImpl.getTokenInfo(token)
            val user: UserDetailsDTO? = userDetailsServiceImpl.getUserByUsername(tokenInfo.username)

            user?.let {
                userDetailsServiceImpl.setUserEnabled(user.username)
                return ResponseEntity.status(HttpStatus.OK).body("Registration completed successful")
            }
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "User not found")

        } catch (error: ErrorResponse) {
            throw ResponseStatusException(error.status, error.errorMessage)
        }
    }



    @PostMapping("/login")
    suspend fun login(@RequestBody login: Login): Jwt {

        // Search if there is a user with the given name
        val user = users.getUserByUsername(login.username)

        user?.let {
            // If the user is not null, we will check if the password provided is the same of the password stored
            if (encoder.matches(login.password, it.password)) {
                // The password is valid, now we check if the user has some limitation that do not allow him to enter
                if (it.userCanAccess()) {
                    // The user has all valid, so we generate the token
                    return Jwt(jwtUtils.generateJwtToken(it))
                } else {
                    // The user cannot log in due to some limitation in his account, we return an error explaining the problem
                    val errorMessage = when {
                        !it.isEnabled -> {
                            // The user inserted a valid email and password, so we know its identity for sure
                            // We send again another email to confirm the account because we suppose the link is expired
                            userDetailsServiceImpl.sendToken(user)
                            "Your account is not enabled. We are sending you another email to confirm"
                        }
                        !it.isAccountNonLocked -> "Your account is locked, contact the support center"
                        !it.isAccountNonExpired -> "Your account expired, contact the support center"
                        !it.isCredentialsNonExpired -> "Your credential is expired, change the password"
                        else -> "Your account has some unknown problem, contact the support center"
                    }

                    throw ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        errorMessage
                    )

                }
            }
        }

        // If the password is not correct, or we do not find any user with that username,
        // we return a generic error to prevent some attacks.
        throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "The combination username and password is not correct"
        )

    }


    @PostMapping("/admin/user")
    suspend fun setUserEnabled(@RequestBody @Valid data: EnableUser): ResponseEntity<String> {
        return try {
            userDetailsServiceImpl.setEnabled(data.username, data.enable)

            if (data.enable) {
                ResponseEntity.status(HttpStatus.OK).body("User ${data.username} was enable successfully")
            } else {
                ResponseEntity.status(HttpStatus.OK).body("User ${data.username} was disabled")
            }

        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.message
            )
        }
    }

    @PostMapping("/admin/makeAdmin")
    suspend fun setUserAdmin(@RequestBody user: Profile): ResponseEntity<String> {
        return try {
            userDetailsServiceImpl.addRole(user.username, Rolename.ADMIN)

            ResponseEntity.status(HttpStatus.OK).body("User ${user.username} is now admin")
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.message
            )
        }
    }

    @PostMapping("/admin/downgradeUser")
    suspend fun downgradeUser(@RequestBody user: Profile): ResponseEntity<String> {
        return try {
            userDetailsServiceImpl.removeRole(user.username, Rolename.ADMIN)

            ResponseEntity.status(HttpStatus.OK).body("User ${user.username} is not admin anymore")
        } catch (e: Exception) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                e.message
            )
        }
    }

    @GetMapping("/admin/{username}")
    suspend fun userInfo(@PathVariable username: String): UserDetailsDTO? {
        // Only the admin can ask information about other users
        return userDetailsServiceImpl.getUserByUsername(username)
    }

}


data class Jwt(val token: String)

@ControllerAdvice
class ValidationHandler {

    // This is the exception launched by @Valid annotation. It will catch and format the errors inside
    @ExceptionHandler(WebExchangeBindException::class)
    fun handleException(e: WebExchangeBindException): ResponseEntity<List<String?>> {
        val errors = e.bindingResult
            .allErrors
            .stream()
            .map { obj: ObjectError -> obj.defaultMessage }
            .collect(Collectors.toList())
        return ResponseEntity.badRequest().body(errors)
    }
}