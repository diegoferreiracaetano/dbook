package com.dbook.domain.catalog

class CatalogEntryInUseException(what: String, val flights: Long) :
    RuntimeException("$what is used by $flights flights and cannot be removed")

class DuplicateIataCodeException(iataCode: String) : RuntimeException("The IATA code $iataCode is already in use")

class CatalogEntryNotFoundException(kind: String, id: Long) : RuntimeException("$kind not found: $id")
