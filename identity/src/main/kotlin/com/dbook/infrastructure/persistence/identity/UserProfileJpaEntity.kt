package com.dbook.infrastructure.persistence.identity

import com.dbook.domain.identity.AppTheme
import com.dbook.domain.identity.CabinClassPreference
import com.dbook.domain.identity.DateFormatPreference
import com.dbook.domain.identity.DistanceUnit
import com.dbook.domain.identity.SeatPreference
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_profile")
class UserProfileJpaEntity(
    @Id
    var userId: Long = 0,
    var avatarUrl: String? = null,
    var language: String? = null,
    @Enumerated(EnumType.STRING)
    var theme: AppTheme? = null,
    var homeAirport: String? = null,
    var country: String? = null,
    var currency: String? = null,
    @Enumerated(EnumType.STRING)
    var cabinClass: CabinClassPreference? = null,
    @Enumerated(EnumType.STRING)
    var seatPreference: SeatPreference? = null,
    @Enumerated(EnumType.STRING)
    var dateFormat: DateFormatPreference? = null,
    @Enumerated(EnumType.STRING)
    var distanceUnit: DistanceUnit? = null,
)
