package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.AnonymizedEmailRepository
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import java.security.MessageDigest
import java.sql.Timestamp
import java.time.Clock

@Repository
class AnonymizedEmailRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
    private val clock: Clock,
) : AnonymizedEmailRepository {
    // joins the transaction of the anonymization that calls it
    override fun remember(email: String) {
        jdbc.update(
            "INSERT INTO anonymized_email (email_hash, anonymized_at) VALUES (:hash, :at) ON CONFLICT DO NOTHING",
            mapOf("hash" to hash(email), "at" to Timestamp.from(clock.instant())),
        )
    }

    override fun isRemembered(email: String): Boolean =
        jdbc.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM anonymized_email WHERE email_hash = :hash)",
            mapOf("hash" to hash(email)),
            Boolean::class.javaObjectType,
        ) == true

    private fun hash(email: String): String =
        MessageDigest.getInstance("SHA-256").digest(email.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
}
