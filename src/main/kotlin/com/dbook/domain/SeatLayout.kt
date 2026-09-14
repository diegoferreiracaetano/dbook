package com.dbook.domain

/**
 * The real seat layout of a known [aircraftType] — a list of column-block sizes, one aisle
 * between each block (e.g. `[3, 3]` is 2 blocks of 3 seats with a single aisle between them;
 * `[3, 4, 3]` is a 3-block widebody row with 2 aisles). This is the ONLY place this mapping
 * lives — both seat generation ([RegisterFlightUseCase]) and the API response
 * ([FlightResponse]) go through it, so the client never needs its own copy of which aircraft
 * maps to which layout.
 */
@Suppress("MagicNumber") // these ARE the named business data — real column counts per aircraft
fun seatLayoutFor(aircraftType: String): List<Int> =
    when (aircraftType) {
        "Embraer E195" -> listOf(2, 2)
        "Boeing 777" -> listOf(3, 4, 3)
        else -> listOf(3, 3) // "Airbus A320" and any other/unknown type default here
    }

/** Every aircraft type [seatLayoutFor] knows a real layout for — used to vary seed data. */
val KNOWN_AIRCRAFT_TYPES = listOf("Embraer E195", "Airbus A320", "Boeing 777")
