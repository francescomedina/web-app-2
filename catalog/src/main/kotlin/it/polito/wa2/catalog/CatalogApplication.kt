package it.polito.wa2.catalog

import it.polito.wa2.catalog.services.CatalogIntegration
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.cloud.client.loadbalancer.LoadBalanced
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

@SpringBootApplication
@ComponentScan("it.polito.wa2")
class CatalogApplication {
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

    @Bean
    @LoadBalanced
    fun loadBalancedWebClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(CatalogApplication::class.java)
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(CatalogApplication::class.java, *args)
        }
    }

//    init {
//        this.threadPoolSize = threadPoolSize
//        this.taskQueueSize = taskQueueSize
//    }
}

fun main(args: Array<String>) {
    SpringApplication.run(CatalogApplication::class.java, *args)
}