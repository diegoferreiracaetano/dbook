package com.dbook.presentation.notification

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingNotificationsMovesTheBadgeAndNeverTouchesSomeoneElsesTest : NotificationFixture() {
    @Test
    fun `given three unread when one is read then the badge drops and unread-only hides it`() {
        val (token, id) = aCustomer()
        repeat(3) { deliver(id) }
        val first = body(inbox(token))["items"][0]["id"].asLong()

        assertEquals(204, markRead(token, first).response.status)

        assertEquals(2, unreadCount(token))
        assertEquals(2, body(inbox(token, "unreadOnly" to "true"))["items"].size())
    }

    @Test
    fun `given someone else's notification when marking it read then it is a 404 and it stays unread`() {
        val (token, _) = aCustomer()
        val (otherToken, otherId) = aCustomer()
        deliver(otherId)
        val others = body(inbox(otherToken))["items"][0]["id"].asLong()

        assertEquals(404, markRead(token, others).response.status)

        assertEquals(1, unreadCount(otherToken))
    }

    @Test
    fun `given unread notifications when all are marked read then the badge is zero`() {
        val (token, id) = aCustomer()
        repeat(2) { deliver(id) }

        val result = markAllRead(token)

        assertEquals(2, body(result)["updated"].asInt())
        assertEquals(0, unreadCount(token))
    }
}
