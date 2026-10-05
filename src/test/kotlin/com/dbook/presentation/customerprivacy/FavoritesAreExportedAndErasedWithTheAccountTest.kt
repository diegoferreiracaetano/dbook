package com.dbook.presentation.customerprivacy

import org.springframework.test.web.servlet.put
import kotlin.test.Test
import kotlin.test.assertEquals

class FavoritesAreExportedAndErasedWithTheAccountTest : CustomerPrivacyFixture() {
    @Test
    fun `given a favorite when exported and then anonymized then the export has it and nothing is left`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email)
        val id = userIdOf(email)
        mockMvc.put("/v1/favorites/DESTINATION/GRU") { header("Authorization", "Bearer $token") }
            .andExpect { status { isNoContent() } }

        val export = bodyOf(myExport(token))
        anonymize(superAdminToken(), id)

        assertEquals(
            listOf("DESTINATION:GRU"),
            export["favorites"].map { "${it["type"].asText()}:${it["id"].asText()}" },
        )
        assertEquals(0, countOf("favorite", "user_id", id))
    }
}
