package com.dbook.domain

class AirlineNotFoundException(iataCode: String) : RuntimeException("Airline not found: $iataCode")
