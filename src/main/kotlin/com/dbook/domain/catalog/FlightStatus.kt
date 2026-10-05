package com.dbook.domain.catalog

/** Whether the flight is on offer. Derived from [Flight.active]: a cancelled flight is simply no longer active. */
enum class FlightStatus { SCHEDULED, CANCELLED }
