package com.dbook.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class ClockConfig {
    // the system zone, not UTC: flights carry local date-times, so "today" must be the server's day
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
