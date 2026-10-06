package com.dbook.infrastructure.ai

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient
import java.time.Duration

@Configuration
class BedrockConfig {
    @Bean
    fun bedrockRuntimeClient(
        @Value("\${ai.bedrock.region}") region: String,
        @Value("\${ai.bedrock.timeout-seconds:10}") timeoutSeconds: Long,
        @Value("\${ai.bedrock.attempt-timeout-seconds:6}") attemptTimeoutSeconds: Long,
    ): BedrockRuntimeClient =
        // Credentials come from the default provider chain (env vars / ~/.aws/credentials
        // / instance profile in ECS) — never configured here, same principle as every
        // other secret in this project.
        BedrockRuntimeClient.builder()
            .region(Region.of(region))
            // an answer that takes longer is not worth waiting for: the customer has the plain search. The whole call
            // (retries included) is bounded, and so is each attempt, so one hung connection cannot eat the budget.
            .overrideConfiguration {
                it.apiCallTimeout(Duration.ofSeconds(timeoutSeconds))
                    .apiCallAttemptTimeout(Duration.ofSeconds(attemptTimeoutSeconds))
            }
            .build()
}
