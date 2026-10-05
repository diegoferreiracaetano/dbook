package com.dbook.application.notification.notificationinbox

import com.dbook.application.notification.NotificationNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReadingMarksOnlyTheCallersNotificationsTest : NotificationInboxFixture() {
    @Test
    fun `given unread notifications when one is read then the unread count drops and unread-only hides it`() {
        markRead.execute(7, 5)

        assertEquals(4, countUnread.execute(7))
        assertEquals(4, list.execute(7, null, 20, unreadOnly = true).items.size)
    }

    @Test
    fun `given someone else's notification when it is marked read then it is not found and stays unread`() {
        assertFailsWith<NotificationNotFoundException> { markRead.execute(7, 6) }

        assertEquals(1, countUnread.execute(8))
    }

    @Test
    fun `given a notification already read when it is marked read again then nothing breaks`() {
        markRead.execute(7, 5)

        markRead.execute(7, 5)

        assertEquals(4, countUnread.execute(7))
    }

    @Test
    fun `given unread notifications when all are marked read then only the caller's are`() {
        assertEquals(5, markAllRead.execute(7))

        assertEquals(0, countUnread.execute(7))
        assertEquals(1, countUnread.execute(8))
    }
}
