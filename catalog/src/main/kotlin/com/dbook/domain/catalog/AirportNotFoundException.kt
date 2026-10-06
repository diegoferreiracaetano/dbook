package com.dbook.domain.catalog

class AirportNotFoundException(iataCode: String) : RuntimeException("Airport not found: $iataCode")
