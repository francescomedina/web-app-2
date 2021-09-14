package it.polito.wa2.catalog.security

import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import reactor.core.publisher.Mono


@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    // With this we can set where are our users stored and which user we have
    @Bean
    fun userDetailsService(encoder: PasswordEncoder): MapReactiveUserDetailsService? {
        // TODO: Change this because is in-memory
        val user: UserDetails = User.builder()
            .username("user")
            .password(encoder.encode("password"))
            .roles("USER")
            .build()

        return MapReactiveUserDetailsService(user)
    }


    // This is the configuration for the security chain
    @Bean
    fun springSecurityFilterChain(
        converter: JwtServerAuthenticationConverter,
        http: ServerHttpSecurity,
        authManager: JwtAuthenticationManager
    ): SecurityWebFilterChain {

        val filter = AuthenticationWebFilter(authManager)
        filter.setServerAuthenticationConverter(converter)

        http
            // It will set inside the header that you need to use the Bearer token (just formality)
            .exceptionHandling()
            .authenticationEntryPoint { exchange, _ ->
                Mono.fromRunnable {
                    exchange.response.statusCode = HttpStatus.UNAUTHORIZED
                    exchange.response.headers.set(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                }
            }

            .and()
            .authorizeExchange()

            // Here we will put the route that do not require authentication
            .pathMatchers(HttpMethod.POST, "/login").permitAll()
            .pathMatchers("/auth/**").permitAll()

            // The other exchange (route) are authenticated
            .anyExchange().authenticated()

            .and()

            // Add the filter to the chain
            .addFilterAt(filter, SecurityWebFiltersOrder.AUTHENTICATION)

            // Disable thing for basic auth because we will use the JWT
            .httpBasic().disable()
            .formLogin().disable()
            // TODO: Check if we need to disable too
            .csrf().disable()


        return http.build()
    }


}