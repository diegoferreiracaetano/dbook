package com.dbook.domain

interface AirportRepository {
    fun findByIataCode(iataCode: String): Airport?
}
