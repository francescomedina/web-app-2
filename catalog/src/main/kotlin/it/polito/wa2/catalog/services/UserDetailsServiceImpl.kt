package it.polito.wa2.catalog.services

import it.polito.wa2.api.exceptions.NotFoundException
import it.polito.wa2.catalog.controller.UserDetailsDTO
import it.polito.wa2.catalog.controller.toUserDetailsDTO
import it.polito.wa2.catalog.controller.toUserEntity
import it.polito.wa2.catalog.persistence.EmailVerificationToken
import it.polito.wa2.catalog.persistence.EmailVerificationTokenRepository
import it.polito.wa2.catalog.persistence.UserRepository
import it.polito.wa2.catalog.security.Rolename
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*


@Service
class UserDetailsServiceImpl(
    private val userRepository: UserRepository,
    private val tokenRepository: EmailVerificationTokenRepository,
    private val notificationService: NotificationService,
    private val mailService: MailService
) : UserDetailsService {

    @Value("\${app.eureka-server}")
    val host: String? = null
    @Value("\${server.port}")
    val port: Int? = 0


    @PreAuthorize("hasAuthority('ROLE_ADMIN')") //TODO: This doesn't work
    /**
     * It will enable/disable the user with such username
     * @param username
     * @param isEnabled : true (if we want to enable) false (if we want to disable)
     */
    suspend fun setEnabled(username: String, isEnabled: Boolean) {
        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {
            user.isEnable = isEnabled
            userRepository.save(user).awaitSingle()

            return
        }

        throw NotFoundException("Username not found")
    }

    /**
     * Create a user with customer role. This user will not be enabled yet.
     * @return the userDetailsDTO of the user
     * @throws ErrorResponse if the email or username already exist
     */
    suspend fun createCustomerUser(userDetailsDTO: UserDetailsDTO): UserDetailsDTO {

        val newUser = userDetailsDTO.toUserEntity()

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

        // We will send the email with the verification token
        sendToken(userDetailsDTO)

        return savedUser.toUserDetailsDTO()
    }

    /**
     * Send the email to the user with the verification token inside in order to enable the user
     * @param userDetailsDTO of the user to enable
     */
    suspend fun sendToken(userDetailsDTO: UserDetailsDTO) {
        // Send confirmation email setting the expiration times to 45 minuted
        val token = notificationService.createEmailVerificationToken(
            Date.from(Instant.now().plus(45, ChronoUnit.MINUTES)),
            userDetailsDTO.username
        ).token

        //TODO: I didn't understand if putting localhost is correct or not.
        val endpoint = "http://localhost:$port/auth/registrationConfirm?token=$token"
        mailService.sendMessage(userDetailsDTO.email, "Confirm Registration", endpoint)
    }

    /**
     * Return UserDetails (not the entity User) of the user passed as parameter
     * @param username
     * @return UserDetails
     * @throws UsernameNotFoundException if the username doesn't exist
     */
    override fun loadUserByUsername(username: String?): UserDetails {

        username?.let {

            val user = userRepository.findByUsername(it).block()
            user?.let {
                return user.toUserDetailsDTO()
            }
        }
        throw UsernameNotFoundException("No user found with username $username")
    }


    /**
     * It will add the roleName to the user with such username
     * @param username
     * @param roleName
     * @throws NotFoundException if the username doesn't exist
     */
    suspend fun addRole(username: String, roleName: Rolename) {

        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {
            user.addRolename(roleName)
            userRepository.save(user).awaitSingle()

            return
        }

        throw NotFoundException("User not found")
    }

    /**
     * It will remove the roleName to the user with such username
     * @param username
     * @param roleName
     * @throws NotFoundException if the username doesn't exist
     */
    suspend fun removeRole(username: String, roleName: Rolename) {

        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {
            user.removeRolename(roleName)
            userRepository.save(user).awaitSingle()
            return
        }

        throw NotFoundException("User not found")
    }

    /**
     * Retrieve the user information and convert to a DTO
     * @param username
     * @return UserDetailsDTO of that username
     */
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

    /**
     * It will take the username and the password (encoded) and will change the password of that username
     * @param username
     * @param password
     * @throws ErrorResponse if some error occurred while saving or user doesn't exist
     */
    suspend fun updatePassword(username: String, password: String) {
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

    /**
     * It will enable the user if the token is not expired
     * @return all the information about that token (if not expired)
     * @throws ErrorResponse if the token is expired
     */
    suspend fun getTokenInfo(token: String): EmailVerificationToken {
        // Get token info from DB
        val tokenInfo = tokenRepository.findByToken(token).awaitSingleOrNull()

        tokenInfo?.let {
            val now = Date.from(Instant.now())
            if (!now.before(it.expiryDate)) {
                // Token date is before now, so the token is expired
                throw ErrorResponse(HttpStatus.BAD_REQUEST, "This token expired, login again to get another one via email")
            }
            return it
        }

        // If we are here, we didn't found such a token
        throw ErrorResponse(HttpStatus.BAD_REQUEST, "Token not found. If you are sure the token is correct, login again to generate a new one")
    }

}

// Exception class used for send to the controller the correct message and status to generate an appropriate http message
data class ErrorResponse(val status: HttpStatus, val errorMessage: String) : Throwable()