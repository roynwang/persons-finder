package com.persons.finder.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.ThreadPoolExecutor

@Configuration
@EnableAsync
class AsyncConfig {

    @Bean("bioTaskExecutor")
    fun bioTaskExecutor(): ThreadPoolTaskExecutor = ThreadPoolTaskExecutor().apply {
        corePoolSize = 2
        maxPoolSize = 4
        setQueueCapacity(50)
        setThreadNamePrefix("bio-")
        // @Async submission runs in the caller's thread: with the default
        // AbortPolicy a saturated queue would throw into the HTTP request and
        // turn a successful person-create into a 500. Bio generation is
        // fire-and-forget, so drop the task instead.
        setRejectedExecutionHandler(ThreadPoolExecutor.DiscardPolicy())
    }
}
