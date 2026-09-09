package com.dbook.infrastructure.web

import io.github.bucket4j.Bandwidth
import io.github.bucket4j.Bucket
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

// AI suggestions call a paid external model (Bedrock) — this limits the blast radius of
// a single user hammering the endpoint, not general API abuse (that's a different
// concern). Scoped to /ai/** only, see WebMvcConfig.
@Component
class AiRateLimitInterceptor : HandlerInterceptor {
    private val bucketsByUserId = ConcurrentHashMap<Long, Bucket>()

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        val userId = SecurityContextHolder.getContext().authentication.name.toLong()
        val bucket = bucketsByUserId.computeIfAbsent(userId) { newBucket() }
        if (bucket.tryConsume(1)) {
            return true
        }
        response.status = HttpStatus.TOO_MANY_REQUESTS.value()
        response.contentType = "application/json"
        response.writer.write("""{"error":"Rate limit exceeded, try again later"}""")
        return false
    }

    private fun newBucket(): Bucket =
        Bucket.builder()
            .addLimit(
                Bandwidth.builder()
                    .capacity(REQUESTS_PER_WINDOW)
                    .refillIntervally(REQUESTS_PER_WINDOW, Duration.ofMinutes(1))
                    .build(),
            )
            .build()

    companion object {
        private const val REQUESTS_PER_WINDOW = 5L
    }
}
