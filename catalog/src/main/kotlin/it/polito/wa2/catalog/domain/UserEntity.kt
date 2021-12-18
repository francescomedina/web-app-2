package it.polito.wa2.catalog.domain

import it.polito.wa2.catalog.security.Rolename
import org.bson.types.ObjectId
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
    var password: String = "",

    var roles: String = "",

    val createdDate: LocalDateTime = LocalDateTime.now(),

    // The user can perform or not some operation
    var isEnable: Boolean = false,

    var name: String = "",
    var surname: String = "",
    var address: String = ""

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