package com.dbook.domain.common

/** The record was changed by someone else since the caller read it: the edit was made on an out-of-date copy. */
class StaleVersionException(message: String = "The record was changed by someone else") : RuntimeException(message)
