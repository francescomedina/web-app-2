package it.polito.wa2.catalog.services

import io.netty.util.internal.SystemPropertyUtil
import it.polito.wa2.api.exceptions.InvalidInputException
import it.polito.wa2.api.exceptions.NotFoundException
import it.polito.wa2.catalog.controller.UserDetailsDTO
import it.polito.wa2.catalog.controller.toUserDetailsDTO
import it.polito.wa2.catalog.persistence.EmailVerificationToken
import it.polito.wa2.catalog.persistence.EmailVerificationTokenRepository
import it.polito.wa2.catalog.persistence.UserEntity
import it.polito.wa2.catalog.persistence.UserRepository
import it.polito.wa2.catalog.security.Rolename
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.withContext

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*


@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository,
    private val tokenRepository: EmailVerificationTokenRepository,
    private val notificationService: NotificationService,
    private val mailService: MailService
) : UserDetailsService {

    @PreAuthorize("hasAuthority('ROLE_ADMIN')") //TODO: This doesn't work
    suspend fun setEnabled(username: String, isEnabled: Boolean) {
        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {
            user.isEnable = isEnabled
            userRepository.save(user).awaitSingle()

            return
        }

        throw NotFoundException("UserDetails not found")
    }

    /**
     * Create a user
     */
    suspend fun createCustomerUser(userDetailsDTO: UserDetailsDTO): UserDetailsDTO {

        // Create the new user
        val newUser = UserEntity(
            username = userDetailsDTO.username,
            password = userDetailsDTO.password,
            email = userDetailsDTO.email,
            roles = userDetailsDTO.roles,
            createdDate = LocalDateTime.now(),
            isEnable = false
        )

        // Store the user inside the DB
        val savedUser = userRepository.save(newUser)
            .onErrorMap { error ->
                throw when {
                    error.message.toString().contains("email dup key") -> ErrorResponse(
                        HttpStatus.CONFLICT,
                        "email already exist"
                    )
                    error.message.toString().contains("username dup key") -> ErrorResponse(
                        HttpStatus.CONFLICT,
                        "username already exist"
                    )
                    else -> ErrorResponse(HttpStatus.BAD_REQUEST, error.message.toString())
                }
            }.awaitSingle()


        // Send confirmation email
        val token = notificationService.createEmailVerificationToken(
            Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)), ///TODO to change into 5 minutes
            userDetailsDTO.username
        ).token

        val endpoint = "http://localhost:7000/auth/registrationConfirm?token=$token"
        mailService.sendMessage(userDetailsDTO.email, "Confirm Registration", endpoint)

        return savedUser.toUserDetailsDTO()
    }

    /**
     * Return UserDetails (not the entity User)
     */
    override fun loadUserByUsername(username: String?): UserDetails {

        username?.let {

            val user = userRepository.findByUsername(it).block()
            user?.let {
                return user.toUserDetailsDTO()
            }
        }
        throw UsernameNotFoundException(String.format("No user found with username '%s'.", username))
    }

    suspend fun addRole(username: String, rolename: Rolename) {

        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {
            user.addRolename(rolename)
            userRepository.save(user).awaitSingle()

            return
        }

        throw NotFoundException("User not found")
    }

    suspend fun removeRole(username: String, rolename: Rolename) {

        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {
            user.removeRolename(rolename)
            userRepository.save(user).awaitSingle()
            return
        }

        throw NotFoundException("User not found")

    }

    // TODO: This is not similar to loadUserByUsername?
    suspend fun getUserByUsername(username: String): UserDetailsDTO? {
        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        return user?.toUserDetailsDTO()
    }

    /**
     * Set the user enable
     * @param username : username of the user
     */
    suspend fun setUserEnabled(username: String) {
        // Find user information
        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {

            // Check if the user is already enabled or not
            if (it.isEnable) {
                throw ErrorResponse(HttpStatus.OK, "User is already enabled")
            }

            // Set the user enable and save it
            user.isEnable = true
            userRepository.save(it).onErrorMap { error ->
                throw ErrorResponse(HttpStatus.BAD_REQUEST, error.message ?: "Generic error")
            }.awaitSingle()
            return
        }
        throw ErrorResponse(HttpStatus.BAD_REQUEST, "User does not exist")
    }

    suspend fun updatePassword(username: String, password: String){
        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {

            // Change user password and save it
            user.password = password
            userRepository.save(it).onErrorMap { error ->
                throw ErrorResponse(HttpStatus.BAD_REQUEST, error.message ?: "Generic error")
            }.awaitSingle()
            return
        }
        throw ErrorResponse(HttpStatus.BAD_REQUEST, "User does not exist")
    }


    suspend fun getTokenInfo(token: String): EmailVerificationToken {

        val tokenInfo = tokenRepository.findByToken(token).awaitSingleOrNull()

        tokenInfo?.let {
            val now = Date.from(Instant.now())
            if (!now.before(it.expiryDate)) {
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "Token expired")
            }
            return it
        }

        throw ErrorResponse(HttpStatus.BAD_REQUEST, "Token not found")
    }


    // With this we can set where are our users stored and which user we have
//    @Bean
//    fun userDetailsService(encoder: PasswordEncoder): MapReactiveUserDetailsService? {
//        // This is in-memory
//        val user: UserDetails = User.builder()
//            .username("user")
//            .password(encoder.encode("password"))
//            .roles("USER")
//            .build()
//
//        return MapReactiveUserDetailsService(user) //TODO: Understand in in reactive it's better use this method


}

data class ErrorResponse(val status: HttpStatus, val errorMessage: String) : Throwable()