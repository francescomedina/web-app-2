package it.polito.wa2.api.composite.catalog


// This class will expose information contained in the User entity to other parts of the system
data class UserInfoJWT(
    private val _username: String = "",
    private val _roles: Set<Rolename> = emptySet(),
) {

    val username : String
        get(){
            return _username
        }

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

    fun isAdmin(): Boolean {
        return _roles.contains(Rolename.ADMIN)
    }

    fun isCustomer(): Boolean {
        return _roles.contains(Rolename.CUSTOMER)
    }
}