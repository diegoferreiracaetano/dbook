package com.dbook.application

import com.dbook.domain.InvalidCredentialsException
import com.dbook.domain.PasswordHasher
import com.dbook.domain.RefreshToken
import com.dbook.domain.RefreshTokenRepository
import com.dbook.domain.Role
import com.dbook.domain.TokenService
import com.dbook.domain.User
import com.dbook.domain.UserRepository
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private class FakeTokenService : TokenService {
    override fun generateAccessToken(user: User): String = "access-${user.id}"

    override fun generateRefreshToken(user: User): String = "refresh-${user.id}-${System.nanoTime()}"

    override fun parseUserId(token: String): Long? = null

    override fun parseRole(token: String): Role? = null

    override fun hashToken(token: String): String = "hash:$token"

    override fun refreshTokenExpiresAt(): Instant = Instant.now().plusSeconds(3600)
}

private class FakeRefreshTokenRepository : RefreshTokenRepository {
    val saved = mutableListOf<RefreshToken>()

    override fun findByTokenHash(tokenHash: String): RefreshToken? = saved.find { it.tokenHash == tokenHash }

    override fun save(refreshToken: RefreshToken): RefreshToken {
        saved += refreshToken
        return refreshToken
    }

    override fun revoke(id: Long) = error("not needed for this test")
}

private class SingleUserRepository(private val user: User) : UserRepository {
    override fun findById(id: Long): User? = user.takeIf { it.id == id }

    override fun findByEmail(email: String): User? = user.takeIf { it.email == email }

    override fun save(user: User): User = user
}

private class FixedPasswordHasher(private val validPassword: String) : PasswordHasher {
    override fun hash(rawPassword: String): String = "hashed:$rawPassword"

    override fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean = rawPassword == validPassword
}

class LoginUseCaseTest {
    private val existingUser =
        User(id = 1, email = "diego@example.com", passwordHash = "irrelevant", role = Role.CLIENT)
    private val refreshTokenRepository = FakeRefreshTokenRepository()
    private val issueTokenPairService = IssueTokenPairService(FakeTokenService(), refreshTokenRepository)
    private val useCase =
        LoginUseCase(
            SingleUserRepository(existingUser),
            FixedPasswordHasher(validPassword = "correct-password"),
            issueTokenPairService,
        )

    @Test
    fun `logs in with correct credentials, persisting the refresh token hash`() {
        val tokens = useCase.execute(LoginCommand(email = "diego@example.com", password = "correct-password"))

        assertEquals("access-1", tokens.accessToken)
        assertEquals(1, refreshTokenRepository.saved.size)
        assertEquals("hash:${tokens.refreshToken}", refreshTokenRepository.saved.first().tokenHash)
    }

    @Test
    fun `rejects an unknown email`() {
        assertFailsWith<InvalidCredentialsException> {
            useCase.execute(LoginCommand(email = "nobody@example.com", password = "correct-password"))
        }
    }

    @Test
    fun `rejects a wrong password`() {
        assertFailsWith<InvalidCredentialsException> {
            useCase.execute(LoginCommand(email = "diego@example.com", password = "wrong-password"))
        }
    }
}
