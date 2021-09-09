package it.polito.wa2.catalog

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.timelimiter.TimeLimiterConfig
import it.polito.wa2.catalog.services.CatalogIntegration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder
import org.springframework.cloud.client.circuitbreaker.Customizer
import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec
import org.springframework.cloud.gateway.route.builder.PredicateSpec
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.netflix.eureka.EnableEurekaClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono


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
            .route("order-route") { it ->
                it.path(true, "/order1/**")
                    .filters { f->
                        f.circuitBreaker {
                                it -> it.setFallbackUri("forward:/failure1")
                        }
                        f.rewritePath("/order1", "/")

                    }

                    .uri("lb://order")
            }

            .build()
    }

    @GetMapping("/failure1")
    fun failure1(): String {
        return "Order service is unavailable"
    }


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
    private val LOG: Logger = LoggerFactory.getLogger(CatalogApplication::class.java)

//    private val threadPoolSize: Int
//    private val taskQueueSize: Int
//
//    @Bean
//    fun publishEventScheduler(): Scheduler {
//        LOG.info("Creates a messagingScheduler with connectionPoolSize = {}", threadPoolSize)
//        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "publish-pool")
//    }

    @Autowired
    var integration: CatalogIntegration? = null
//    @Bean
//    fun coreServices(): ReactiveHealthContributor {
//        val registry: MutableMap<String, ReactiveHealthIndicator> = LinkedHashMap()
//        registry["product"] = ReactiveHealthIndicator { integration.getProductHealth() }
//        registry["recommendation"] = ReactiveHealthIndicator { integration.getRecommendationHealth() }
//        registry["review"] = ReactiveHealthIndicator { integration.getReviewHealth() }
//        return CompositeReactiveHealthContributor.fromMap(registry)
//    }

//    @Bean
//    @LoadBalanced
//    fun loadBalancedWebClientBuilder(): WebClient.Builder {
//        return WebClient.builder()
//    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CatalogApplication::class.java)
    }

//    init {
//        this.threadPoolSize = threadPoolSize
//        this.taskQueueSize = taskQueueSize
//    }
}

fun main(args: Array<String>) {
    SpringApplication.run(CatalogApplication::class.java, *args)
}