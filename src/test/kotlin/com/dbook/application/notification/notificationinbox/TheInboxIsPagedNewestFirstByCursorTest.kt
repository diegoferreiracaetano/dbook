package com.dbook.application.notification.notificationinbox

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TheInboxIsPagedNewestFirstByCursorTest : NotificationInboxFixture() {
    @Test
    fun `given five notifications when paging by two then each page continues where the last one stopped`() {
        val first = list.execute(7, before = null, size = 2, unreadOnly = false)
        val second = list.execute(7, before = first.next, size = 2, unreadOnly = false)
        val third = list.execute(7, before = second.next, size = 2, unreadOnly = false)

        assertEquals(listOf("mine 4", "mine 3"), first.items.map { it.title })
        assertEquals(listOf("mine 2", "mine 1"), second.items.map { it.title })
        assertEquals(listOf("mine 0"), third.items.map { it.title })
        assertNull(third.next)
    }

    @Test
    fun `given someone else's notifications when listing then only the caller's are returned`() {
        val page = list.execute(8, before = null, size = 20, unreadOnly = false)

        assertEquals(listOf("someone else's"), page.items.map { it.title })
    }
}
