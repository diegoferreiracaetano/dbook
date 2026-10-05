package com.dbook.presentation.hardening

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.read.ListAppender
import com.dbook.presentation.customerprofile.CustomerProfileFixture
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.post
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Everything the application logs while a customer registers, signs in (and fails to), refreshes, books, pays and is
// refused is collected from the root logger. None of it may carry the e-mail, the name, the password, a token, the
// cardholder or the card digits. This is the test that catches the log line someone adds next month.
class NoPersonalDataOrSecretReachesTheLogsTest : CustomerProfileFixture() {
    private val appender = ListAppender<ILoggingEvent>()
    private val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    @BeforeTest
    fun listen() {
        appender.start()
        root.addAppender(appender)
    }

    @AfterTest
    fun stopListening() {
        root.detachAppender(appender)
        appender.stop()
    }

    private fun everythingLogged(): String =
        appender.list.toList().joinToString("\n") { event ->
            listOf(
                event.formattedMessage,
                event.mdcPropertyMap.toString(),
                event.argumentArray?.joinToString().orEmpty(),
                event.throwableProxy?.let(ThrowableProxyUtil::asString).orEmpty(),
            ).joinToString(" ")
        }

    @Test
    fun `given a customer's whole journey when the logs are read then no secret or personal data is in them`() {
        val email = uniqueEmail()
        val name = "Maria Confidencial"
        val password = "s3cret-password-42"
        mockMvc.post("/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to password, "name" to name))
        }
        mockMvc.post("/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("email" to email, "password" to "wrong-password-99"))
        }
        val access = loginAccessToken(email, password)
        val refresh = loginRefreshToken(email, password)
        mockMvc.post("/v1/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(mapOf("refreshToken" to refresh))
        }
        val (booking) = bookSeats(access, 1)
        pay(access, booking)

        val logs = everythingLogged()

        assertTrue(appender.list.isNotEmpty(), "the test collected nothing: it would pass for the wrong reason")
        listOf(email, name, password, "wrong-password-99", access, refresh, "Jane Doe").forEach {
            assertEquals(false, logs.contains(it), "the logs contain '${it.take(12)}...'")
        }
    }
}
