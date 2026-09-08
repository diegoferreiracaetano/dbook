package com.dbook.domain

class BookableNotFoundException(id: Long) : RuntimeException("Bookable not found: $id")
