package com.dbook.infrastructure.messaging.expiration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import java.net.URI

@Configuration
class SqsConfig {
    @Bean
    fun sqsClient(
        @Value("\${aws.sqs.region}") region: String,
        @Value("\${aws.sqs.endpoint:}") endpoint: String,
    ): SqsClient {
        val builder = SqsClient.builder().region(Region.of(region))
        // An endpoint is only ever set against LocalStack, which accepts any credentials; in
        // AWS it stays empty and the default provider chain applies (same as BedrockConfig).
        if (endpoint.isNotBlank()) {
            builder
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
        }
        return builder.build()
    }
}
