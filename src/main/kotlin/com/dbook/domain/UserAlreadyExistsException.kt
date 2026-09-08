package com.dbook.domain

class UserAlreadyExistsException(email: String) : RuntimeException("User already exists: $email")
