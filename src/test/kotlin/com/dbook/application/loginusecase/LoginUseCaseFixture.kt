package com.dbook.application.loginusecase

import com.dbook.application.IssueTokenPairService
import com.dbook.application.LoginUseCase
import com.dbook.domain.PasswordHasher
import com.dbook.domain.RefreshToken
import com.dbook.domain.RefreshTokenRepository
import com.dbook.domain.Role
import com.dbook.domain.TokenService
import com.dbook.domain.User
import com.dbook.domain.UserRepository
import java.time.Instant

class FakeTokenService : TokenService {
    override fun generateAccessToken(user: User): String = "access-${user.id}"

    override fun generateRefreshToken(user: User): String = "refresh-${user.id}-${System.nanoTime()}"

    override fun parseUserId(token: String): Long? = null

    override fun parseRole(token: String): Role? = null

    override fun hashToken(token: String): String = "hash:$token"

    override fun refreshTokenExpiresAt(): Instant = Instant.now().plusSeconds(3600)
}

class FakeRefreshTokenRepository : RefreshTokenRepository {
    val saved = mutableListOf<RefreshToken>()

    override fun findByTokenHash(tokenHash: String): RefreshToken? = saved.find { it.tokenHash == tokenHash }

    override fun save(refreshToken: RefreshToken): RefreshToken {
        saved += refreshToken
        return refreshToken
    }

    override fun revoke(id: Long) = error("not needed for this test")
}

class SingleUserRepository(private val user: User) : UserRepository {
    override fun findById(id: Long): User? = user.takeIf { it.id == id }

    override fun findByEmail(email: String): User? = user.takeIf { it.email == email }

    override fun save(user: User): User = user
}

class FixedPasswordHasher(private val validPassword: String) : PasswordHasher {
    override fun hash(rawPassword: String): String = "hashed:$rawPassword"

    override fun matches(
        rawPassword: String,
        hash: String,
    ): Boolean = rawPassword == validPassword
}

// Shared "given" for every LoginUseCase scenario below: one existing CLIENT user whose
// correct password is "correct-password".
abstract class LoginUseCaseFixture {
    protected val existingUser =
        User(id = 1, email = "diego@example.com", passwordHash = "irrelevant", role = Role.CLIENT)
    protected val refreshTokenRepository = FakeRefreshTokenRepository()
    private val issueTokenPairService = IssueTokenPairService(FakeTokenService(), refreshTokenRepository)
    protected val useCase =
        LoginUseCase(
            SingleUserRepository(existingUser),
            FixedPasswordHasher(validPassword = "correct-password"),
            issueTokenPairService,
        )
}
