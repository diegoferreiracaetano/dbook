package com.dbook.presentation.staffmanagement

import kotlin.test.Test
import kotlin.test.assertEquals

class AnyBadTokenGetsTheSameAnswerTest : StaffManagementFixture() {
    @Test
    fun `given an unknown token and a used one when accepting then both are the same 400 INVALID_INVITATION`() {
        val adminToken = newSuperAdminToken()
        val email = uniqueEmail()
        invite(adminToken, email).andExpect { status { isCreated() } }
        val token = tokenMailedTo(email)
        accept(token).andExpect { status { isCreated() } }

        val used = accept(token).andReturn()
        val unknown = accept("not-a-real-token").andReturn()

        assertEquals(400, used.response.status)
        assertEquals(400, unknown.response.status)
        assertEquals("INVALID_INVITATION", errorCodeOf(used))
        assertEquals(used.response.contentAsString, unknown.response.contentAsString)
    }
}
