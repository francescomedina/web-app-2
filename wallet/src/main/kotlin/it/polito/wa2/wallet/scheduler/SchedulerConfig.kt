package it.polito.wa2.wallet.scheduler

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
/*
@Configuration
class SchedulerConfig @Autowired constructor(
    @Value("\${app.threadPoolSize:10}") threadPoolSize: Int,
    @Value("\${app.taskQueueSize:100}") taskQueueSize: Int
) {

    private val threadPoolSize: Int
    private val taskQueueSize: Int

    @Bean
    fun publishEventScheduler(): Scheduler {
        return Schedulers.newBoundedElastic(threadPoolSize, taskQueueSize, "publish-pool")
    }

    init {
        this.threadPoolSize = threadPoolSize
        this.taskQueueSize = taskQueueSize
    }
}

 */