package com.dbook.application.crm.exportmydatausecase

import com.dbook.domain.identity.AppTheme
import com.dbook.domain.identity.UserPreferences
import com.dbook.domain.identity.UserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TheExportCarriesThePictureAndThePreferencesTest : ExportMyDataUseCaseFixture() {
    @Test
    fun `given a picture and preferences when exporting then they come, and with none the profile is empty`() {
        assertNull(useCase.execute(3).profile.avatarUrl)

        profiles.save(
            UserProfile(3, "https://cdn.example.com/me.jpg", UserPreferences(language = "en", theme = AppTheme.DARK)),
        )
        val export = useCase.execute(3)

        assertEquals("https://cdn.example.com/me.jpg", export.profile.avatarUrl)
        assertEquals("en", export.profile.preferences.language)
        assertEquals(AppTheme.DARK, export.profile.preferences.theme)
    }
}
