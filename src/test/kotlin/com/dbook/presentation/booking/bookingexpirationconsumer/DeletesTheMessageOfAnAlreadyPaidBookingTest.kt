package com.dbook.presentation.booking.bookingexpirationconsumer

import com.dbook.LocalStackSqs
import com.dbook.domain.booking.BookingStatus
import com.dbook.domain.seating.SeatStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeletesTheMessageOfAnAlreadyPaidBookingTest : BookingExpirationConsumerFixture() {
    @Test
    fun `given a booking paid in time when its message arrives then it stays confirmed and the message is deleted`() {
        bookingRepository.save(requireNotNull(bookingRepository.findById(bookingId)).confirm(paymentId = 1L))
        val (queueUrl, _) =
            LocalStackSqs.createQueueWithDeadLetterQueue(
                visibilityTimeoutSeconds = 1,
                maxReceiveCount = 5,
            )
        LocalStackSqs.send(queueUrl, """{"bookingId":$bookingId}""")

        withTransactionSynchronization { consumerFor(queueUrl).poll() }
        waitForRedelivery()

        assertEquals(BookingStatus.CONFIRMED, bookingRepository.findById(bookingId)?.status)
        assertEquals(SeatStatus.RESERVED, seatRepository.findById(seatId)?.status)
        assertTrue(LocalStackSqs.receive(queueUrl, waitSeconds = 0).isEmpty(), "a no-op still counts as handled")
    }
}
