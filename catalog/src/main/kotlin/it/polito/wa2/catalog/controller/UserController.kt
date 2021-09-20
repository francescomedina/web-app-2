package it.polito.wa2.catalog.controller

import io.netty.util.internal.SystemPropertyUtil.contains
import it.polito.wa2.api.exceptions.InvalidInputException
import it.polito.wa2.catalog.DTO.RegistrationBody
import it.polito.wa2.catalog.persistence.UserEntity
import it.polito.wa2.catalog.persistence.UserRepository
import it.polito.wa2.catalog.security.InvalidBearerToken
import it.polito.wa2.catalog.security.JwtAuthenticationManager
import it.polito.wa2.catalog.security.JwtUtils
import it.polito.wa2.catalog.security.Rolename
import it.polito.wa2.catalog.services.ErrorResponse
import it.polito.wa2.catalog.services.UserDetailsServiceImpl
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.ReactiveUserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.validation.ObjectError
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.support.WebExchangeBindException
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Mono
import java.security.Principal
import java.util.stream.Collectors
import javax.validation.Valid


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
                "The confirmation password doesn't match with the password insert"
            )
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


    @GetMapping("/me")
    suspend fun me(@AuthenticationPrincipal principal: Principal): Profile {
        // Do not publish the principal inside the response directly; expose only the information needed
        return Profile(principal.name)
    }

    @GetMapping("/user/{username}")
    suspend fun userInfo(@PathVariable username: String): UserEntity? {
        val user = userRepository.findByUsername(username).awaitSingleOrNull()
        return user
    }

    @PostMapping("/login")
    suspend fun login(@RequestBody login: Login): Jwt {

        // Search if there is a user with the given name
        val user = users.getUserByUsername(login.username)

        user?.let {
            // If the user is not null we will check if the password provided is the same of the password stored
            if (encoder.matches(login.password, it.password)) {
                // The password is valid, so we return the JWT
                if (it.userCanAccess()) {
                    return Jwt(jwtUtils.generateJwtToken(it))
                } else {
                    throw ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Your account has some problem"
                    ) //TODO: Better error handling
                }
            }
        }

        throw ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "The combination username and password is not correct"
        )

    }

}


data class Jwt(val token: String)

// This is the representation of what the user will send during the login phase
data class Login(val username: String, val password: String)

data class Profile(val username: String)

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