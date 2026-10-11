package com.dbook.infrastructure.persistence.crm

import com.dbook.domain.crm.CustomerErasure
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class CustomerErasureAdapter(
    private val jdbc: NamedParameterJdbcTemplate,
) : CustomerErasure {
    // joins the transaction of the anonymization that calls it: everything leaves together or nothing does
    override fun erasePersonalData(customerId: Long) {
        val params = mapOf("id" to customerId)
        jdbc.update("DELETE FROM customer_note WHERE customer_id = :id", params)
        jdbc.update("DELETE FROM ai_suggestion_log WHERE user_id = :id", params)
        jdbc.update("DELETE FROM notification WHERE user_id = :id", params)
        jdbc.update("DELETE FROM notification_preference WHERE user_id = :id", params)
        jdbc.update("DELETE FROM device_token WHERE user_id = :id", params)
        jdbc.update("DELETE FROM user_profile WHERE user_id = :id", params)
        jdbc.update("DELETE FROM favorite WHERE user_id = :id", params)
        jdbc.update("DELETE FROM price_alert WHERE user_id = :id", params)
        jdbc.update("UPDATE payment SET cardholder_name = 'ANONYMIZED' WHERE customer_id = :id", params)
    }
}
