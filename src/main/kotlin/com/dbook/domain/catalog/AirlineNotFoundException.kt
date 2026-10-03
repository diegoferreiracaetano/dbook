package com.dbook.domain.catalog

class AirlineNotFoundException(iataCode: String) : RuntimeException("Airline not found: $iataCode")
