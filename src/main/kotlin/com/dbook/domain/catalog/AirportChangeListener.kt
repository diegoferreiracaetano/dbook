package com.dbook.domain.catalog

/**
 * Who must know that an airport was created or changed: whatever keeps a copy of data that shows airports (the flight
 * search keeps its results for a few seconds, with each flight's airports in them). The catalog does not know who
 * listens.
 */
interface AirportChangeListener {
    fun airportChanged()
}
