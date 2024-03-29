package it.polito.wa2.catalog

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController


@SpringBootApplication
@EnableEurekaClient
@ComponentScan("it.polito.wa2")
@RestController
class CatalogApplication {

    @Bean
    fun defaultCustomizer(): Customizer<ReactiveResilience4JCircuitBreakerFactory> {
        return Customizer { factory ->
            factory.configureDefault { id ->
                Resilience4JConfigBuilder(id)
                    .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
                    .timeLimiterConfig(TimeLimiterConfig.ofDefaults())
                    .build()
            }
        }
    }

    @Bean
    fun routes(builder: RouteLocatorBuilder): RouteLocator {
        return builder
            .routes()
            .route("wallet-route") { it ->
                it.path(true, "/wallet-composite/**")
                    .filters { f->
                        f.circuitBreaker {
                                it -> it.setFallbackUri("forward:/wallet-failure")
                        }
                        f.rewritePath("/wallet-composite", "/wallets")

                    }

                    .uri("lb://wallet")
            }
            .route("order-route") { it ->
                it.path(true, "/order-composite/**")
                    .filters { f->
                        f.circuitBreaker {
                                it -> it.setFallbackUri("forward:/order-failure")
                        }

                        f.rewritePath("/order-composite", "/orders")

                    }

                    .uri("lb://order")
            }
            .route("warehouse-route") { it ->
                it.path(true, "/warehouse-composite/**")
                    .filters { f->
                        f.circuitBreaker {
                                it -> it.setFallbackUri("forward:/warehouse-failure")
                        }
                        f.rewritePath("/warehouse-composite", "/warehouses")

                    }
                    .uri("lb://warehouse")
            }
            .route("products-route") { it ->
                it.path(true, "/products-composite/**")
                    .filters { f->
                        f.circuitBreaker {
                                it -> it.setFallbackUri("forward:/warehouse-failure")
                        }
                        f.rewritePath("/products-composite", "/products")

                    }
                    .uri("lb://warehouse")
            }
            .build()
    }

    @GetMapping("/order-failure")
    fun orderFailure(): String {
        return "Order service is unavailable"
    }

    @GetMapping("/wallet-failure")
    fun walletFailure(): String {
        return "Wallet service is unavailable"
    }

    @GetMapping("/warehouse-failure")
    fun warehouseFailure(): String {
        return "Warehouse service is unavailable"
    }

    /** START JAVA MAIL SENDER **/

    @Value("\${spring.mail.host}")
    val host: String? = null
    @Value("\${spring.mail.port}")
    val port: Int? = 0
    @Value("\${spring.mail.username}")
    val username: String? = null
    @Value("\${spring.mail.password}")
    val password: String? = null
    @Value("\${spring.mail.properties.mail.smtp.auth}")
    val auth: String? = null
    @Value("\${spring.mail.properties.mail.smtp.starttls.enable}")
    val enable: String? = null
    @Value("\${spring.mail.properties.mail.debug}")
    val debug: String? = null

    @Bean
    fun getMailSender(): JavaMailSender {

        val mailSender = JavaMailSenderImpl()
        mailSender.host = host
        mailSender.port = port ?: 8080

        mailSender.username = username
        mailSender.password = password

        val props = mailSender.javaMailProperties
        props["mail.transport.protocol"] = "smtp"
        props["mail.smtp.auth"] = auth
        props["mail.smtp.starttls.enable"] = enable
        props["mail.debug"] = debug

        return mailSender
    }


    /** END JAVA MAIL SENDER **/

    @Value("\${api.common.version}")
    var apiVersion: String? = null

    @Value("\${api.common.title}")
    var apiTitle: String? = null

    @Value("\${api.common.description}")
    var apiDescription: String? = null

    @Value("\${api.common.termsOfService}")
    var apiTermsOfService: String? = null

    @Value("\${api.common.license}")
    var apiLicense: String? = null

    @Value("\${api.common.licenseUrl}")
    var apiLicenseUrl: String? = null

    @Value("\${api.common.externalDocDesc}")
    var apiExternalDocDesc: String? = null

    @Value("\${api.common.externalDocUrl}")
    var apiExternalDocUrl: String? = null

    @Value("\${api.common.contact.name}")
    var apiContactName: String? = null

    @Value("\${api.common.contact.url}")
    var apiContactUrl: String? = null

    @Value("\${api.common.contact.email}")
    var apiContactEmail: String? = null

}

fun main(args: Array<String>) {
    SpringApplication.run(CatalogApplication::class.java, *args)
}