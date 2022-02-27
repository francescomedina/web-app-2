package it.polito.wa2.catalog.security

import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.AuthenticationWebFilter
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import reactor.core.publisher.Mono

@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    //It will provide the best encoding at the moment (we don't need to put a specific one that can became obsolete)
    fun passwordEncoder(): PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()


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

            // Here we will put the route with a specific permission
            .pathMatchers("/auth/admin/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers("/auth/user/**").hasAuthority(Rolename.CUSTOMER.toString())

            .pathMatchers("/auth/**").permitAll()

//            .pathMatchers(HttpMethod.PATCH, "/order-composite/**").hasAuthority(Rolename.ADMIN.toString()) // User can CANCEL (delete) an order but can not modify products and other attributes
//            .pathMatchers("/order-composite/**").hasAuthority(Rolename.CUSTOMER.toString())
//            .pathMatchers(HttpMethod.PATCH,"/order-composite/**").permitAll()
            .pathMatchers(HttpMethod.POST, "/order-composite/**").hasAuthority(Rolename.CUSTOMER.toString())
            .pathMatchers(HttpMethod.PATCH, "/order-composite/**").hasAuthority(Rolename.CUSTOMER.toString())
            .pathMatchers(HttpMethod.DELETE, "/order-composite/**").hasAuthority(Rolename.CUSTOMER.toString())
            .pathMatchers(HttpMethod.GET, "/order-composite/**").permitAll()

            .pathMatchers(HttpMethod.POST, "/products-composite/addRating/**").permitAll()
            .pathMatchers(HttpMethod.POST, "/products-composite/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers(HttpMethod.PATCH, "/products-composite/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers(HttpMethod.PUT, "/products-composite/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers(HttpMethod.DELETE, "/products-composite/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers(HttpMethod.GET, "/products-composite/**").permitAll()

            .pathMatchers(HttpMethod.POST, "/warehouse-composite/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers(HttpMethod.PATCH, "/warehouse-composite/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers(HttpMethod.PUT, "/warehouse-composite/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers(HttpMethod.DELETE, "/warehouse-composite/**").hasAuthority(Rolename.ADMIN.toString())
            .pathMatchers(HttpMethod.GET, "/warehouse-composite/**").permitAll()

            // The other exchange (route) are authenticated
            .anyExchange().authenticated()

            .and()

            // Add the filter to the chain
            .addFilterAt(filter, SecurityWebFiltersOrder.AUTHENTICATION)

            // Disable thing for basic auth because we will use the JWT
            .httpBasic().disable()
            .formLogin().disable()

            .csrf().disable()

            // This is similar to "http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)"
            // for Spring security reactive. See: https://github.com/spring-projects/spring-security/issues/6552#issuecomment-519398510
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())


        return http.build()
    }


}