package it.polito.wa2.order

import it.polito.wa2.order.persistence.OrderEntity
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.data.mongodb.core.ReactiveMongoOperations
import org.springframework.data.mongodb.core.index.IndexDefinition
import org.springframework.data.mongodb.core.index.IndexResolver
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver
import org.springframework.data.mongodb.core.index.ReactiveIndexOperations


@SpringBootApplication
@ComponentScan("it.polito.wa2")
class OrderApplication {
    @Autowired
    var mongoTemplate: ReactiveMongoOperations? = null
    @EventListener(ContextRefreshedEvent::class)
    fun initIndicesAfterStartup() {
        val mappingContext = mongoTemplate!!.converter.mappingContext
        val resolver: IndexResolver = MongoPersistentEntityIndexResolver(mappingContext)
        val indexOps: ReactiveIndexOperations = mongoTemplate!!.indexOps(OrderEntity::class.java)
        resolver.resolveIndexFor(OrderEntity::class.java).forEach { e: IndexDefinition? ->
            indexOps.ensureIndex(e!!).block()
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(OrderApplication::class.java)
        @JvmStatic
        fun main(args: Array<String>) {
            val ctx = SpringApplication.run(OrderApplication::class.java, *args)
            val mongodDbHost = ctx.environment.getProperty("spring.data.mongodb.host")
            val mongodDbPort = ctx.environment.getProperty("spring.data.mongodb.port")
            LOG.info("Connected to MongoDb: $mongodDbHost:$mongodDbPort")
        }
    }
}

fun main(args: Array<String>) {
    runApplication<OrderApplication>(*args)
}
