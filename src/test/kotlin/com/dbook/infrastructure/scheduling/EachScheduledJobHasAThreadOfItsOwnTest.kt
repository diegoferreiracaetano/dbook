package com.dbook.infrastructure.scheduling

import com.dbook.AbstractIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.core.type.filter.AnnotationTypeFilter
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.stereotype.Component
import org.springframework.test.util.ReflectionTestUtils
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertTrue

// The queue consumers long-poll, so a job can hold its thread for many seconds. With a single scheduler thread the
// outbox relay would stop while a consumer waits; the pool must have a thread for every job there is.
class EachScheduledJobHasAThreadOfItsOwnTest : AbstractIntegrationTest() {
    @Autowired
    @Qualifier("taskScheduler")
    lateinit var scheduler: ThreadPoolTaskScheduler

    @Autowired
    lateinit var processor: ScheduledAnnotationBeanPostProcessor

    // poolSize is how many threads exist right now (they are made on demand), not how many are allowed
    private fun configuredThreads() = scheduler.scheduledThreadPoolExecutor.corePoolSize

    private fun scheduledJobs(): Int =
        ClassPathScanningCandidateComponentProvider(false)
            .apply { addIncludeFilter(AnnotationTypeFilter(Component::class.java)) }
            .findCandidateComponents("com.dbook")
            .map { Class.forName(it.beanClassName) }
            .sumOf {
                    type ->
                type.declaredMethods.count { AnnotatedElementUtils.hasAnnotation(it, Scheduled::class.java) }
            }

    @Test
    fun `given the scheduled jobs of the app when the pool is read then it has a thread for each of them`() {
        val jobs = scheduledJobs()

        assertTrue(jobs >= 4, "the scan found only $jobs jobs: the test is not looking where the jobs are")
        assertTrue(configuredThreads() >= jobs, "$jobs scheduled jobs and only ${configuredThreads()} threads")
    }

    @Test
    fun `given the scheduled jobs when the app starts then they run on that pool and not on the websocket one`() {
        val registrar = ReflectionTestUtils.getField(processor, "registrar") as ScheduledTaskRegistrar

        val thread = CompletableFuture<String>()
        registrar.scheduler!!.schedule({ thread.complete(Thread.currentThread().name) }, Instant.now())

        assertTrue(thread.get(5, TimeUnit.SECONDS).startsWith("dbook-sched-"))
    }

    @Test
    fun `given jobs stuck waiting when another job is due then it still runs`() {
        val stuck = CountDownLatch(1)
        val ran = CountDownLatch(1)
        try {
            repeat(configuredThreads() - 1) { scheduler.schedule({ stuck.await() }, Instant.now()) }

            scheduler.schedule({ ran.countDown() }, Instant.now())

            assertTrue(ran.await(5, TimeUnit.SECONDS), "a job waited behind the stuck ones")
        } finally {
            stuck.countDown()
        }
    }
}
