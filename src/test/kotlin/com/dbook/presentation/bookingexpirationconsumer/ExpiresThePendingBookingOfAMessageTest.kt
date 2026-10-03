package com.dbook.presentation.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import com.dbook.domain.BookingStatus
import com.dbook.domain.SeatStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpiresThePendingBookingOfAMessageTest : BookingExpirationConsumerFixture() {
    @Test
    fun `given a message for a pending booking when polling then it expires the booking and deletes the message`() {
        val (queueUrl, _) =
            LocalStackSqs.createQueueWithDeadLetterQueue(
                visibilityTimeoutSeconds = 1,
                maxReceiveCount = 5,
            )
        LocalStackSqs.send(queueUrl, """{"bookingId":$bookingId}""")

        withTransactionSynchronization { consumerFor(queueUrl).poll() }
        waitForRedelivery()

        assertEquals(BookingStatus.CANCELLED, bookingRepository.findById(bookingId)?.status)
        assertEquals(SeatStatus.AVAILABLE, seatRepository.findById(seatId)?.status)
        assertTrue(LocalStackSqs.receive(queueUrl, waitSeconds = 0).isEmpty(), "the message must have been deleted")
    }
}
