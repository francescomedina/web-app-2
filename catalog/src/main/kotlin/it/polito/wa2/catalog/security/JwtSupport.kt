package it.polito.wa2.catalog.security

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

// This represents the token that the user receive when authenticated and send to access a protected resource.
// Inside we can store anything in various format. In general, it is stored as JWT.
class BearerToken(val value: String) : AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
    override fun getCredentials(): Any = value
    override fun getPrincipal(): Any = value
}

@Component
class JwtSupport {
    // Secret key to sign the JWT
    private val key = Keys.hmacShaKeyFor("tNO+KhVrTj3B4q0+SEwz/NSvZq7y577jOjvY4uPgAR4=".toByteArray())

    // Parse the JWT provider
    private val parser =Jwts.parserBuilder().setSigningKey(key).build()

    // This function will generate a Bearer token for the user
    fun generate(username: String): BearerToken {
        val builder = Jwts.builder()
            .setSubject(username)
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
            .signWith(key)

        // We will build the JWT using the builder, and we return it
        return BearerToken(builder.compact())
    }

    // Return the username from the BearerToken
    fun getUsername(token: BearerToken): String {
        return parser.parseClaimsJws(token.value).body.subject
    }

    // It will validate the JWT the user provide
    fun isValid(token: BearerToken, user: UserDetails?): Boolean {
        val claims = parser.parseClaimsJws(token.value).body
        val unexpired = claims.expiration.after(Date.from(Instant.now()))

        return unexpired && (claims.subject == user?.username)
    }


}