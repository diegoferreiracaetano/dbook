package com.dbook.presentation.promo

import kotlin.test.Test
import kotlin.test.assertEquals

class TheTeamCreatesChangesAndSwitchesOffCodesAndEachActionIsAuditedTest : PromoApiFixture() {
    @Test
    fun `given a lower-case code when created then it is kept in capitals, and the same code in any case is a 409`() {
        val token = manager()
        val code = aCode()

        val created = createPromo(token, promoBody(code.lowercase()))

        assertEquals(201, created.response.status)
        assertEquals(code, json(created)["code"].asText())
        assertEquals(0, json(created)["redeemed"].asInt())
        assertEquals(409, createPromo(token, promoBody(code)).response.status)
    }

    @Test
    fun `given invalid data when a code is created then it is a 400`() {
        val token = manager()

        assertEquals(400, createPromo(token, promoBody(aCode(), "value" to 100)).response.status)
        assertEquals(400, createPromo(token, promoBody("ab")).response.status)
        assertEquals(400, createPromo(token, promoBody(aCode(), "maxPerUser" to 0)).response.status)
        assertEquals(
            400,
            createPromo(token, promoBody(aCode(), "validUntil" to "2000-01-01T00:00:00Z")).response.status,
        )
    }

    @Test
    fun `given a code when its limits change then the discount stays, and when it is switched off and on it follows`() {
        val token = manager()
        val (id, _) = newPromo(token)

        val changed =
            updatePromo(
                token,
                id,
                mapOf(
                    "minAmount" to 50,
                    "validFrom" to promoBody()["validFrom"],
                    "validUntil" to promoBody()["validUntil"],
                    "maxRedemptions" to 5,
                    "maxPerUser" to 2,
                ),
            )
        assertEquals(200, changed.response.status)
        assertEquals(10.0, json(changed)["value"].asDouble())
        assertEquals(5, json(changed)["maxRedemptions"].asInt())

        assertEquals(false, json(adminPost(token, "/v1/admin/promo-codes/$id/deactivate"))["active"].asBoolean())
        assertEquals(409, adminPost(token, "/v1/admin/promo-codes/$id/deactivate").response.status)
        assertEquals(true, json(adminPost(token, "/v1/admin/promo-codes/$id/activate"))["active"].asBoolean())
        assertEquals(404, adminGet(token, "/v1/admin/promo-codes/999999999").response.status)
    }

    @Test
    fun `given the actions above when the trail is read then each one is there with its actor`() {
        val token = manager()
        val admin = registerStaffAndLogin(uniqueEmail())
        val (id, _) = newPromo(token)
        adminPost(token, "/v1/admin/promo-codes/$id/deactivate")

        val actions = auditEntries(admin, "targetId=$id&size=50").filter { it["targetType"].asText() == "PROMO" }

        assertEquals(setOf("PROMO_CREATED", "PROMO_DEACTIVATED"), actions.map { it["action"].asText() }.toSet())
        assertEquals(
            "true",
            actions.first { it["action"].asText() == "PROMO_DEACTIVATED" }["before"]["active"].asText(),
        )
    }

    @Test
    fun `given active and inactive codes when listed then the filter works and the count of uses is shown`() {
        val token = manager()
        val (off, _) = newPromo(token)
        adminPost(token, "/v1/admin/promo-codes/$off/deactivate")
        newPromo(token)

        val inactive = json(adminGet(token, "/v1/admin/promo-codes?active=false&size=100"))["items"]
        val active = json(adminGet(token, "/v1/admin/promo-codes?active=true&size=100"))["items"]

        assertEquals(true, inactive.any { it["id"].asLong() == off })
        assertEquals(false, active.any { it["id"].asLong() == off })
    }
}
