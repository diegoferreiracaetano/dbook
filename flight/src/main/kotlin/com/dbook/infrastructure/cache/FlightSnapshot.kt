package com.dbook.infrastructure.cache

import com.dbook.domain.catalog.Airport
import com.dbook.domain.flight.Airline
import com.dbook.domain.flight.Flight
import com.dbook.domain.flight.SeatClass
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import java.math.BigDecimal
import java.time.LocalDateTime

// A flight written to JSON and read back by hand. The API's mapper has no Kotlin module, so it cannot rebuild these
// classes by itself, and a cache that quietly zeroes a field is worse than no cache: the test of the round trip
// compares every field.
class FlightSnapshot(private val mapper: ObjectMapper) {
    fun write(flights: List<Flight>): String = mapper.writeValueAsString(flights.map(::toNode))

    fun read(json: String): List<Flight> = mapper.readTree(json).map(::fromNode)

    private fun toNode(flight: Flight): ObjectNode =
        mapper.createObjectNode().apply {
            put("id", flight.id)
            put("title", flight.title)
            put("price", flight.price.toPlainString())
            put("totalCapacity", flight.totalCapacity)
            put("availableCapacity", flight.availableCapacity)
            put("active", flight.active)
            put("flightNumber", flight.flightNumber)
            set<ObjectNode>("airline", airlineNode(flight.airline))
            set<ObjectNode>("origin", airportNode(flight.origin))
            set<ObjectNode>("destination", airportNode(flight.destination))
            put("departureTime", flight.departureTime.toString())
            put("arrivalTime", flight.arrivalTime.toString())
            put("seatClass", flight.seatClass.name)
            put("aircraftType", flight.aircraftType)
        }

    private fun airlineNode(airline: Airline): ObjectNode =
        mapper.createObjectNode().apply {
            put("id", airline.id)
            put("iataCode", airline.iataCode)
            put("name", airline.name)
            put("logoUrl", airline.logoUrl)
        }

    private fun airportNode(airport: Airport): ObjectNode =
        mapper.createObjectNode().apply {
            put("id", airport.id)
            put("iataCode", airport.iataCode)
            put("name", airport.name)
            put("city", airport.city)
            put("country", airport.country)
            put("photoUrl", airport.photoUrl)
            put("region", airport.region)
            put("isPopular", airport.isPopular)
        }

    private fun fromNode(node: JsonNode): Flight =
        Flight(
            id = node["id"].asLongOrNull(),
            title = node["title"].asText(),
            price = BigDecimal(node["price"].asText()),
            totalCapacity = node["totalCapacity"].asInt(),
            availableCapacity = node["availableCapacity"].asInt(),
            active = node["active"].asBoolean(),
            flightNumber = node["flightNumber"].asText(),
            airline = airline(node["airline"]),
            origin = airport(node["origin"]),
            destination = airport(node["destination"]),
            departureTime = LocalDateTime.parse(node["departureTime"].asText()),
            arrivalTime = LocalDateTime.parse(node["arrivalTime"].asText()),
            seatClass = SeatClass.valueOf(node["seatClass"].asText()),
            aircraftType = node["aircraftType"].asText(),
        )

    private fun airline(node: JsonNode) =
        Airline(
            node["id"].asLongOrNull(),
            node["iataCode"].asText(),
            node["name"].asText(),
            node["logoUrl"]?.takeUnless { it.isNull }?.asText(),
        )

    private fun airport(node: JsonNode) =
        Airport(
            id = node["id"].asLongOrNull(),
            iataCode = node["iataCode"].asText(),
            name = node["name"].asText(),
            city = node["city"].asText(),
            country = node["country"].asText(),
            photoUrl = node["photoUrl"].asText(),
            region = node["region"].asText(),
            isPopular = node["isPopular"].asBoolean(),
        )

    private fun JsonNode.asLongOrNull(): Long? = takeUnless { it.isNull }?.asLong()
}
