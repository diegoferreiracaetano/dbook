package com.dbook.domain.identity

class UserAlreadyExistsException(email: String) : RuntimeException("User already exists: $email")
