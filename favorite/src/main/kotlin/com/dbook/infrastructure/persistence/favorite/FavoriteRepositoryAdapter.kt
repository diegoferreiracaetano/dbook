package com.dbook.infrastructure.persistence.favorite

import com.dbook.domain.favorite.Favorite
import com.dbook.domain.favorite.FavoriteAddResult
import com.dbook.domain.favorite.FavoriteRepository
import com.dbook.domain.favorite.FavoriteType
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp

@Repository
class FavoriteRepositoryAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : FavoriteRepository {
    // The customer's row is locked first: two requests of the same customer line up behind each other, so counting and
    // inserting cannot both see "199" and both insert. Other customers are not held up.
    @Transactional
    override fun add(
        favorite: Favorite,
        limit: Int,
    ): FavoriteAddResult {
        val params = mapOf("user" to favorite.userId, "type" to favorite.type.name, "target" to favorite.targetId)
        jdbc.queryForList("SELECT id FROM app_user WHERE id = :user FOR UPDATE", params, Long::class.javaObjectType)
        val already =
            jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM favorite WHERE user_id = :user AND target_type = :type " +
                    "AND target_id = :target)",
                params,
                Boolean::class.javaObjectType,
            ) == true
        val count =
            jdbc.queryForObject(
                "SELECT count(*) FROM favorite WHERE user_id = :user",
                params,
                Long::class.javaObjectType,
            )
        return when {
            already -> FavoriteAddResult.ALREADY_FAVORITE
            (count ?: 0L) >= limit -> FavoriteAddResult.LIMIT_REACHED
            else -> {
                jdbc.update(
                    "INSERT INTO favorite (user_id, target_type, target_id, created_at) " +
                        "VALUES (:user, :type, :target, :now)",
                    params + ("now" to Timestamp.from(favorite.createdAt)),
                )
                FavoriteAddResult.ADDED
            }
        }
    }

    @Transactional
    override fun remove(
        userId: Long,
        type: FavoriteType,
        targetId: String,
    ): Boolean =
        jdbc.update(
            "DELETE FROM favorite WHERE user_id = :user AND target_type = :type AND target_id = :target",
            mapOf("user" to userId, "type" to type.name, "target" to targetId),
        ) > 0
}
