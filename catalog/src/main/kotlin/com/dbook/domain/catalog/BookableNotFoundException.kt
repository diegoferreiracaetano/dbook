package com.dbook.domain.catalog

class BookableNotFoundException(id: Long) : RuntimeException("Bookable not found: $id")
