package com.dbook.presentation.common.swagger

import com.dbook.infrastructure.observability.ObservabilityFixture
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals

// The Swagger docs are split into one group per API version; the examples of the parameters are
// added by a customizer, which has to be applied to the groups too (it silently was not).
class EachVersionGroupCarriesTheParameterExamplesTest : ObservabilityFixture() {
    @Test
    fun `given the v1 swagger group when it is read then the parameter examples are there`() {
        val v1 = ObjectMapper().readTree(getFromApi("/v3/api-docs/v1").body())

        val paymentParameters = v1["paths"]["/v1/payments"]["post"]["parameters"]
        val idempotencyKey = paymentParameters.single { it["name"].asText() == "Idempotency-Key" }
        assertEquals("3f2b8c1e-6a4d-4e7a-9d1b-5c8e2a7f0b94", idempotencyKey["example"].asText())

        val searchParameters = v1["paths"]["/v1/flights/search"]["get"]["parameters"]
        assertEquals("GRU", searchParameters.single { it["name"].asText() == "origin" }["example"].asText())
    }
}
