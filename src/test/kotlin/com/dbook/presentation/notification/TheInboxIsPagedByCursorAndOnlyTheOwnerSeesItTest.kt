package com.dbook.presentation.notification

import org.springframework.test.web.servlet.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TheInboxIsPagedByCursorAndOnlyTheOwnerSeesItTest : NotificationFixture() {
    @Test
    fun `given three notifications when paging by two then the cursor leads to the last one and then ends`() {
        val (token, id) = aCustomer()
        repeat(3) { deliver(id) }

        val first = body(inbox(token, "size" to "2"))
        val second = body(inbox(token, "size" to "2", "cursor" to first["nextCursor"].asText()))

        assertEquals(2, first["items"].size())
        assertEquals(1, second["items"].size())
        assertNull(second["nextCursor"].takeIf { !it.isNull })
        assertEquals("Reserva confirmada", first["items"][0]["title"].asText())
        assertEquals(false, first["items"][0]["read"].asBoolean())
    }

    @Test
    fun `given another customer's notifications when listing then only my own appear`() {
        val (token, _) = aCustomer()
        val (_, otherId) = aCustomer()
        deliver(otherId)

        assertEquals(0, body(inbox(token))["items"].size())
    }

    @Test
    fun `given no token when listing then it is a 401`() {
        assertEquals(401, mockMvc.get("/v1/notifications").andReturn().response.status)
    }
}
