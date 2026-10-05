package com.dbook.infrastructure.scheduling

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler

/**
 * The threads of the `@Scheduled` jobs: the outbox relay and its cleanup, and the queue consumers, which long-poll and
 * so can hold a thread for many seconds. Left alone, they ran on the WebSocket broker's scheduler (the only
 * `TaskScheduler` in the context, a pool of its own with heartbeats to send), so a slow queue could delay the
 * heartbeats of every open socket and the jobs delayed each other. Here they get a pool of their own with a thread
 * per job and some room: a job that hangs holds its own thread and nobody else's. The bean must be called
 * `taskScheduler`: that is the name Spring looks for when there is more than one scheduler.
 */
@Configuration
class SchedulingConfig {
    @Bean(name = ["taskScheduler"])
    fun taskScheduler(
        @Value("\${scheduling.pool-size:6}") poolSize: Int,
    ): ThreadPoolTaskScheduler =
        ThreadPoolTaskScheduler().apply {
            this.poolSize = poolSize
            setThreadNamePrefix("dbook-sched-")
            setWaitForTasksToCompleteOnShutdown(false)
        }
}
