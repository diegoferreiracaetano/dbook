package com.dbook.presentation.securityintegration

import org.springframework.http.MediaType
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import kotlin.test.Test

class TheOwnPreferencesAvatarAndPasswordAreKeptAndValidatedTest : SecurityIntegrationFixture() {
    @Test
    fun `given a new account when reading the preferences then every one is null and the profile has its date`() {
        val token = registerAndLogin(uniqueEmail())

        mockMvc.get("/v1/users/me/preferences") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.language") { value(null) }
            jsonPath("$.currency") { value(null) }
        }
        mockMvc.get("/v1/users/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            jsonPath("$.createdAt") { exists() }
            jsonPath("$.avatarUrl") { value(null) }
        }
    }

    @Test
    fun `given preferences when saved then they come back, and a whole replacement clears what was left out`() {
        val token = registerAndLogin(uniqueEmail())
        val all =
            mapOf(
                "language" to "pt-BR",
                "theme" to "DARK",
                "homeAirport" to "gru",
                "country" to "br",
                "currency" to "BRL",
                "cabinClass" to "BUSINESS",
                "seatPreference" to "WINDOW",
                "dateFormat" to "DMY",
                "distanceUnit" to "KM",
            )
        putJson("/v1/users/me/preferences", token, all).andExpect {
            status { isOk() }
            jsonPath("$.homeAirport") { value("GRU") }
            jsonPath("$.country") { value("BR") }
            jsonPath("$.theme") { value("DARK") }
        }

        putJson("/v1/users/me/preferences", token, mapOf("language" to "en")).andExpect {
            jsonPath("$.language") { value("en") }
            jsonPath("$.theme") { value(null) }
            jsonPath("$.currency") { value(null) }
        }
    }

    @Test
    fun `given an unknown value when saving the preferences then it is a 400 and nothing is kept`() {
        val token = registerAndLogin(uniqueEmail())

        for (bad in listOf(
            mapOf("language" to "xx"),
            mapOf("currency" to "JPY"),
            mapOf("homeAirport" to "GRUU"),
            mapOf("country" to "BRA"),
            mapOf("theme" to "NEON"),
        )) {
            putJson("/v1/users/me/preferences", token, bad).andExpect { status { isBadRequest() } }
        }
        mockMvc.get("/v1/users/me/preferences") { header("Authorization", "Bearer $token") }
            .andExpect { jsonPath("$.language") { value(null) } }
    }

    @Test
    fun `given the avatar when set, replaced and removed then only an https url is accepted`() {
        val token = registerAndLogin(uniqueEmail())

        putJson("/v1/users/me/avatar", token, mapOf("avatarUrl" to "https://cdn.example.com/me.jpg")).andExpect {
            status { isOk() }
            jsonPath("$.avatarUrl") { value("https://cdn.example.com/me.jpg") }
        }
        putJson("/v1/users/me/avatar", token, mapOf("avatarUrl" to "http://cdn.example.com/me.jpg"))
            .andExpect { status { isBadRequest() } }
        mockMvc.delete("/v1/users/me/avatar") { header("Authorization", "Bearer $token") }.andExpect {
            status { isOk() }
            jsonPath("$.avatarUrl") { value(null) }
        }
    }

    @Test
    fun `given the current password when changing it then the new one logs in and the old one does not`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email, password = "s3cret-password")

        mockMvc.post("/v1/users/me/password") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("currentPassword" to "s3cret-password", "newPassword" to "another-secret-1"),
                )
        }.andExpect { status { isNoContent() } }

        loginAccessToken(email, "another-secret-1")
        mockMvc.post("/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to "s3cret-password"))
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `given a wrong current password when changing it then it is refused and the password stays`() {
        val email = uniqueEmail()
        val token = registerAndLogin(email, password = "s3cret-password")

        mockMvc.post("/v1/users/me/password") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content =
                objectMapper.writeValueAsString(
                    mapOf("currentPassword" to "not-it-at-all", "newPassword" to "another-secret-1"),
                )
        }.andExpect { status { is4xxClientError() } }

        loginAccessToken(email, "s3cret-password")
    }

    @Test
    fun `given no token when using the new endpoints then they are all 401`() {
        mockMvc.get("/v1/users/me/preferences").andExpect { status { isUnauthorized() } }
        mockMvc.put("/v1/users/me/preferences") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isUnauthorized() } }
        mockMvc.delete("/v1/users/me/avatar").andExpect { status { isUnauthorized() } }
        mockMvc.post("/v1/users/me/password") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect { status { isUnauthorized() } }
    }

    private fun putJson(
        path: String,
        token: String,
        body: Map<String, String>,
    ) = mockMvc.put(path) {
        header("Authorization", "Bearer $token")
        contentType = MediaType.APPLICATION_JSON
        content = objectMapper.writeValueAsString(body)
    }
}
