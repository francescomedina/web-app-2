package it.polito.wa2.catalog.security

import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.security.SignatureException
import it.polito.wa2.catalog.controller.UserDetailsDTO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.annotation.PostConstruct

// This represents the token that the user receive when authenticated and send to access a protected resource.
// Inside we can store anything in various format. In general, it is stored as JWT.
class BearerToken(val value: String) : AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
    override fun getCredentials(): Any = value
    override fun getPrincipal(): Any = value
}

@Component
class JwtUtils(



) {
    @Value("\${application.jwt.jwtSecret}")
    val secret: String = ""
    @Value("\${application.jwt.jwtExpirationMs}")
    val jwtExpirationInMs = 0L

    init {
        // Secret key to sign the JWT

        //val key = Keys.hmacShaKeyFor(secret.toByteArray())

        // Parse the JWT provider
        //val parser =Jwts.parserBuilder().setSigningKey(key).build()
    }



    // This function will generate a Bearer token for the user
//    fun generate(username: String): BearerToken {
//        val builder = Jwts.builder()
//            .setSubject(username)
//            .setIssuedAt(Date.from(Instant.now()))
//            .setExpiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
//            .signWith(key)
//
//        // We will build the JWT using the builder, and we return it
//        return BearerToken(builder.compact())
//    }


    /**
     * It will generate the JWT Token with inside the user details
     * FIXME: Inside lab3 the professor said that is takes a authentication: Authentication; i don't know how to get
     */
    fun generateJwtToken(user: UserDetails): String{
        val roles = user.authorities

        // Create the claims object
        val claims: MutableMap<String, Any> = HashMap()
        claims["isAdmin"] = roles.contains(SimpleGrantedAuthority(Rolename.ADMIN.name))
        claims["isCustomer"] = roles.contains(SimpleGrantedAuthority(Rolename.CUSTOMER.name))

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.username)
            .setIssuedAt(Date.from(Instant.now()))
            .setExpiration(Date.from(Instant.now().plus(jwtExpirationInMs, ChronoUnit.MILLIS)))
            .signWith(Keys.hmacShaKeyFor(secret.toByteArray()))
            .compact()
    }


    fun validateJwtToken(authToken: String?): Boolean {
        try {
            val key = Keys.hmacShaKeyFor(secret.toByteArray())
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken).body
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


    fun getDetailsFromJwtToken (authToken: String): UserDetailsDTO {
        //TODO: This can be factorize above
        val key = Keys.hmacShaKeyFor(secret.toByteArray())
        val claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken).body


        val roles = mutableSetOf<Rolename>()
        if(claims["isAdmin"]==true){
            roles.add(Rolename.ADMIN)
        }
        if(claims["isCustomer"]==true){
            roles.add(Rolename.CUSTOMER)
        }

        return UserDetailsDTO(_username=claims.subject, _roles=roles)
    }




}