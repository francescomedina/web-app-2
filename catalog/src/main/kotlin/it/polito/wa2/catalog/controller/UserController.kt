package it.polito.wa2.catalog.controller

import io.netty.util.internal.SystemPropertyUtil.contains
import it.polito.wa2.api.exceptions.InvalidInputException
import it.polito.wa2.catalog.DTO.RegistrationBody
import it.polito.wa2.catalog.persistence.UserEntity
import it.polito.wa2.catalog.persistence.UserRepository
import it.polito.wa2.catalog.security.JwtSupport
import it.polito.wa2.catalog.security.Rolename
import it.polito.wa2.catalog.services.ErrorResponse
import it.polito.wa2.catalog.services.UserDetailsServiceImpl
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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
    private val jwtSupport: JwtSupport,
    private val encoder: PasswordEncoder,
    private val users: ReactiveUserDetailsService,

    private val userDetailsServiceImpl: UserDetailsServiceImpl,
    private val userRepository: UserRepository

) {

    /**
     * Controller that handle the registration process
     * It will check if the password and confirmPassword are the same and then delegate the creation to the service above
     */
    @PostMapping("/register")
    suspend fun register(@RequestBody @Valid data: Mono<RegistrationBody>): ResponseEntity<Mono<UserDetailsDTO>> {

        data.awaitSingleOrNull()?.let { it ->
            if (it.password == it.confirmPassword) {

                // Mapping the information inside RegistrationBody with a UserDetailsDTO format
                val userDTO = UserDetailsDTO(
                    _username = it.username,
                    _password = it.password,
                    _email = it.email,
                    _roles = setOf(Rolename.CUSTOMER),
                    isEnable = false
                )

                // Talking to the service above to create the user
                val createdUser = userDetailsServiceImpl.createCustomerUser(userDTO)
                    .onErrorMap(ErrorResponse::class.java) { error: ErrorResponse ->
                        // An error occurred during the user creation
                        throw ResponseStatusException(error.status, error.errorMessage)
                    }

                // Return a 201 with inside the user created
                return ResponseEntity.status(HttpStatus.CREATED).body(createdUser)
            }
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "The confirmation password doesn't match with the password insert"
            )
        }

        throw ResponseStatusException(HttpStatus.BAD_REQUEST)
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

        // Search if there is a user with the given name            // We need await because returns a Mono
        val user = users.findByUsername(login.username).awaitSingleOrNull()

        user?.let {
            // If the user is not null we will check if the password provided is the same of the password stored
            if (encoder.matches(login.password, it.password)) {
                // The password is valid, so we return the JWT
                return Jwt(jwtSupport.generate(it.username).value)
            }
        }

        throw ResponseStatusException(HttpStatus.UNAUTHORIZED)

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