package com.dbook.domain.flight

class AirlineNotFoundException(iataCode: String) : RuntimeException("Airline not found: $iataCode")
