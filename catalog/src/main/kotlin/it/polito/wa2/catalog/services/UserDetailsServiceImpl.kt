package it.polito.wa2.catalog.services

import io.netty.util.internal.SystemPropertyUtil
import it.polito.wa2.api.exceptions.InvalidInputException
import it.polito.wa2.api.exceptions.NotFoundException
import it.polito.wa2.catalog.controller.UserDetailsDTO
import it.polito.wa2.catalog.controller.toUserDetailsDTO
import it.polito.wa2.catalog.persistence.UserEntity
import it.polito.wa2.catalog.persistence.UserRepository
import it.polito.wa2.catalog.security.Rolename
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.dao.DuplicateKeyException
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.lang.IllegalArgumentException
import java.sql.Timestamp
import java.time.LocalDateTime

@Service
class UserDetailsServiceImpl(

    private val userRepository: UserRepository

) : UserDetailsService {

//    @PreAuthorize("hasAuthority('ADMIN')")
//    fun setEnabled(username: String, isEnabled: Boolean){
//        val user = userRepository.findByUsername(username)?: throw NotFoundException("UserDetails not found")
//        user.isEnabled = isEnabled
//        userRepository.save(user)
//    }

    /**
     * Create a user
     */
    fun createCustomerUser(userDetailsDTO: UserDetailsDTO): Mono<UserDetailsDTO> {

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
                    error.message.toString().contains("email dup key") -> ErrorResponse(HttpStatus.CONFLICT, "email already exist")
                    error.message.toString().contains("username dup key") -> ErrorResponse(HttpStatus.CONFLICT, "username already exist")
                    else -> ErrorResponse(HttpStatus.BAD_REQUEST, error.message.toString())
                }
            }
            .mapNotNull { e -> e.toUserDetailsDTO() }

        return savedUser
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
            userRepository.save(user)
        }

        throw NotFoundException("User not found")
    }

    suspend fun removeRole(username: String, rolename: Rolename) {

        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        user?.let {
            user.removeRolename(rolename)
            userRepository.save(user)
        }

        throw NotFoundException("User not found")

    }

    suspend fun getUserByUsername(username: String): UserDetailsDTO? {
        val user = userRepository.findByUsername(username).awaitSingleOrNull()

        return user?.toUserDetailsDTO()
    }




}

data class  ErrorResponse(val status: HttpStatus, val errorMessage: String): Throwable()