package it.polito.wa2.util.jwt

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import it.polito.wa2.api.composite.catalog.Rolename
import it.polito.wa2.api.composite.catalog.UserInfoJWT

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JwtValidateUtils {
    @Value("\${application.jwt.jwtSecret}")
    val secret: String = ""

    @Value("\${application.jwt.jwtExpirationMs}")
    val jwtExpirationInMs = 0L

    private fun validateJwtToken(authToken: String?): Boolean {
        try {

            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken?.substring(7)).body
            return true
        } catch (e: SignatureException) {
            println("Invalid JWT signature: {} ${e.message}")
        } catch (e: MalformedJwtException) {
            println("Invalid JWT token: {} ${e.message}")
        } catch (e: ExpiredJwtException) {
            println("JWT token is expired: {} ${e.message}")
        } catch (e: UnsupportedJwtException) {
            println("JWT token is unsupported: {} ${e.message}")
        } catch (e: IllegalArgumentException) {
            println("JWT claims string is empty: {} ${e.message}")
        }
        return false
    }


    fun getDetailsFromJwtToken(authToken: String): UserInfoJWT {

        if (validateJwtToken(authToken)) {

            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            val claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken?.substring(7)).body


            val roles = mutableSetOf<Rolename>()
            if (claims["isAdmin"] == true) {
                roles.add(Rolename.ADMIN)
            }
            if (claims["isCustomer"] == true) {
                roles.add(Rolename.CUSTOMER)
            }

            return UserInfoJWT(_username = claims.subject, _roles = roles)
        }

        throw Exception("JWT not valid")

    }
}