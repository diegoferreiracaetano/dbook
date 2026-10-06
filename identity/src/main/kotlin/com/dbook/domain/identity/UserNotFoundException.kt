package com.dbook.domain.identity

class UserNotFoundException(id: Long) : RuntimeException("User not found: $id")
