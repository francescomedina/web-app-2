package it.polito.wa2.catalog.persistence

import it.polito.wa2.catalog.security.Rolename
import org.bson.types.ObjectId
import org.hibernate.validator.constraints.UniqueElements
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("user")
data class UserEntity(

    @Id
    val id: ObjectId = ObjectId.get(),

    @Indexed(unique = true)
    val username: String,
    val password: String = "",

    @Indexed(unique = true, name = "unique-email")
    val email: String = "",
    var roles: String = "", //TODO: Take away this =.. and make something inside service that handle the mapping

    //var customer: Customer?, TODO: Understand the meaning

    val createdDate: LocalDateTime = LocalDateTime.now(),

    // The user can perform or not some operation
    var isEnable: Boolean = false,
    //TODO: Check other thing for registration

    ) {

    fun getRolenames(): Set<Rolename> {
        if (roles == "")
            return setOf<Rolename>()
        return roles.split(", ").map {
            Rolename.valueOf(it)
        }.toSet()
    }

    fun addRolename(rolename: Rolename) {
        if (!getRolenames().contains(rolename)) {
            roles = if (roles == "")
                "$rolename"
            else
                "$roles, $rolename"
        }
    }

    fun removeRolename(rolename: Rolename) {
        val rolenames = getRolenames().toMutableSet()
        rolenames.remove(rolename)
        roles = ""
        rolenames.forEach {
            println(it)
            addRolename(it)
        }
    }
}