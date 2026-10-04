package com.dbook.presentation.common.errorcode

import com.dbook.presentation.common.ErrorCode
import kotlin.test.Test

// SecurityResponses and AiRateLimitInterceptor live in infrastructure and write these strings by hand (they
// cannot import presentation); renaming one side without the other would silently change the contract.
class TheCodesWrittenByTheSecurityLayerExistInTheEnumTest {
    @Test
    fun `given the codes infrastructure writes as text when looked up in the enum then every one exists`() {
        listOf("UNAUTHORIZED", "FORBIDDEN", "RATE_LIMITED").forEach { ErrorCode.valueOf(it) }
    }
}
