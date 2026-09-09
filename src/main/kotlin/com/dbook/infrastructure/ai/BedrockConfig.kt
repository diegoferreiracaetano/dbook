package com.dbook.infrastructure.ai

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient

@Configuration
class BedrockConfig {
    @Bean
    fun bedrockRuntimeClient(
        @Value("\${ai.bedrock.region}") region: String,
    ): BedrockRuntimeClient =
        // Credentials come from the default provider chain (env vars / ~/.aws/credentials
        // / instance profile in ECS) — never configured here, same principle as every
        // other secret in this project.
        BedrockRuntimeClient.builder()
            .region(Region.of(region))
            .build()
}
