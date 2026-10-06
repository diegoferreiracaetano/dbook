package com.dbook.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor

/**
 * The thread that sends the mails that must not slow the answer (the password reset: whether an account exists must
 * not show in how long the request takes). A small pool with a bounded queue: a flood of requests is dropped by the
 * request limits long before it fills it. (Spring Boot's own `applicationTaskExecutor` does not exist here: it steps
 * aside when another executor is defined, and the scheduler of the `@Scheduled` jobs is one.)
 */
@Configuration
@EnableAsync
class AsyncConfig {
    @Bean("mailExecutor")
    fun mailExecutor(): ThreadPoolTaskExecutor =
        ThreadPoolTaskExecutor().apply {
            corePoolSize = CORE_THREADS
            maxPoolSize = MAX_THREADS
            queueCapacity = QUEUE_CAPACITY
            setThreadNamePrefix("dbook-mail-")
            setWaitForTasksToCompleteOnShutdown(true)
        }

    private companion object {
        const val CORE_THREADS = 2
        const val MAX_THREADS = 4
        const val QUEUE_CAPACITY = 100
    }
}
