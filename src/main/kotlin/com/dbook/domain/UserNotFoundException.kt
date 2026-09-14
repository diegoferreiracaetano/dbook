package com.dbook.domain

class UserNotFoundException(id: Long) : RuntimeException("User not found: $id")
