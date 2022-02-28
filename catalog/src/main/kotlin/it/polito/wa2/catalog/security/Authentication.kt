package it.polito.wa2.catalog.security

import kotlinx.coroutines.reactor.mono
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.lang.IllegalArgumentException


@Component
class JwtServerAuthenticationConverter : ServerAuthenticationConverter {

    override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
        // Retrieve the JWT from the Authorization header removing the Bearer prefix
        return Mono.justOrEmpty(exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION))
            .filter { it.startsWith("Bearer ") }
            .map { it.substring(7) }
            .map { jwt -> BearerToken(jwt) }
    }
}

@Component
class JwtAuthenticationManager(
    private val jwtUtils: JwtUtils,
) : ReactiveAuthenticationManager {

    override fun authenticate(authentication: Authentication?): Mono<Authentication> {
        return Mono.justOrEmpty(authentication)
            .filter { auth -> auth is BearerToken }
            .cast(BearerToken::class.java)
            .flatMap { jwt ->
                mono { validate(jwt) }
            }
            .onErrorMap { error ->
                InvalidBearerToken(error.message)
            }

    }

    private fun validate(token: BearerToken): Authentication {

        if (jwtUtils.validateJwtToken(token.value)) {
            // Extract the user details information contained inside JWT
            val userDetailsDTO = jwtUtils.getDetailsFromJwtToken(token.value)

            // Create a UsernamePasswordAuthenticationToken object, setting the username and granted authorities
            // fetched from the JWT, while leaving the password to null
            val authentication = UsernamePasswordAuthenticationToken(userDetailsDTO, null, userDetailsDTO.authorities)

            // Add extra details coming from the request
            //authentication.details = WebAuthenticationDetailsSource().buildDetails(request)
            //Set the Authentication object in the SecurityContext
            //SecurityContextHolder.getContext().authentication = authentication
            ReactiveSecurityContextHolder.withAuthentication(authentication)

            return authentication
        }

        throw IllegalArgumentException("Token is not valid.")
    }

}

class InvalidBearerToken(message: String?) : AuthenticationException(message)