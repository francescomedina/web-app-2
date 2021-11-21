package it.polito.wa2.catalog.controller

import it.polito.wa2.catalog.domain.UserEntity
import it.polito.wa2.catalog.security.Rolename
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.time.LocalDateTime
import javax.validation.constraints.NotBlank

// This class will expose information contained in the User entity to other parts of the system
data class UserDetailsDTO(
    private val _username: String = "",
    private val _password: String = "",

       private val _email: String = "",
    private val _roles: Set<Rolename>,
    private val accountNonExpired: Boolean = true,
    private val accountNonLocked: Boolean = true,
    private val credentialsNonExpired: Boolean = true,
    private val isEnable: Boolean = false,

    val name: String = "",
    val surname: String = "",
    val address: String = "",
) : UserDetails {

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> {
        return _roles
            .map { SimpleGrantedAuthority(it.name) }
            .toMutableList()
    }

    override fun getPassword(): String {
        return _password
    }

    override fun getUsername(): String {
        return _username
    }

    val email: String get() = _email

    val roles: String
        get() {
            var output = ""
            _roles.forEach {
                if (output == "")
                    output = "$it"
                else
                    output = "$output, $it"
            }
            return output
        }

    override fun isAccountNonExpired(): Boolean {
        return accountNonExpired
    }

    override fun isAccountNonLocked(): Boolean {
        return accountNonLocked
    }

    override fun isCredentialsNonExpired(): Boolean {
        return credentialsNonExpired
    }

    override fun isEnabled(): Boolean {
        return isEnable
    }

    fun userCanAccess(): Boolean {
        return isAccountNonExpired && isAccountNonLocked && isCredentialsNonExpired && isEnable
    }

}


// USER ENTITY --> USER DETAILS DTO
// Convert a user entity into a user DTO. The method created has the name of toUserDetailsDTO
fun UserEntity.toUserDetailsDTO() = UserDetailsDTO(

    _username = username,
    _password = password,
    _roles = getRolenames(),
    _email = email,
    isEnable = isEnable,
    accountNonExpired = true,
    accountNonLocked = true,
    credentialsNonExpired = true,
    name = name,
    surname = surname,
    address = address
)

// USER DTO --> USER ENTITY
fun UserDetailsDTO.toUserEntity() = UserEntity(
    username = username,
    password = password,
    email = email,
    roles = roles,
    createdDate = LocalDateTime.now(),
    isEnable = isEnabled,
    name = name,
    surname = surname,
    address = address
)