package com.dbook.domain

class AirportNotFoundException(iataCode: String) : RuntimeException("Airport not found: $iataCode")
