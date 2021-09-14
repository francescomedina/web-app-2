package it.polito.wa2.catalog.controller

import it.polito.wa2.catalog.persistence.UserEntity
import it.polito.wa2.catalog.security.Rolename
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

// This class will expose information contained in the User entity to other parts of the system
data class UserDetailsDTO(
    private val _username: String,
    private val _password: String,
    private val _email: String,
    private val _roles: Set<Rolename>,
    private val accountNonExpired: Boolean = true, //TODO: Change this later
    private val accountNonLocked: Boolean = true,
    private val credentialsNonExpired: Boolean = true,
    private val isEnable: Boolean,
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
            var output: String = ""
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


}


fun UserEntity.toUserDetailsDTO() = UserDetailsDTO(

    _username = username,
    _password = password,
    _roles = getRolenames(),
    _email = email,
    isEnable = isEnable,
    accountNonExpired = true,
    accountNonLocked = true,
    credentialsNonExpired = true

)