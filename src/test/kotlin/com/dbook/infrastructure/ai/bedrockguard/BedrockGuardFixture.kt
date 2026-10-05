package com.dbook.infrastructure.ai.bedrockguard

import com.dbook.infrastructure.ai.BedrockGuard
import com.dbook.infrastructure.ai.BedrockGuardSettings
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import software.amazon.awssdk.core.exception.SdkClientException

abstract class BedrockGuardFixture {
    protected val meters = SimpleMeterRegistry()

    protected fun guardOf(settings: BedrockGuardSettings = BedrockGuardSettings()) = BedrockGuard(settings, meters)

    protected fun count(outcome: String): Double = meters.counter("dbook.ai.bedrock.calls", "outcome", outcome).count()

    // what the SDK throws when the service is down or the call times out
    protected fun serviceDown(): Nothing = throw SdkClientException.create("the model is down")
}
