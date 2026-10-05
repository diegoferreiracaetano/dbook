package com.dbook.application.notification.notificationinbox

import kotlin.test.Test
import kotlin.test.assertFailsWith

class ThePageSizeIsBoundedTest : NotificationInboxFixture() {
    @Test
    fun `given a size of zero or above the maximum when listing then it is refused`() {
        assertFailsWith<IllegalArgumentException> { list.execute(7, null, 0, false) }
        assertFailsWith<IllegalArgumentException> { list.execute(7, null, 1_000, false) }
    }
}
