package com.dbook.domain.payment

class RefundNotFoundException(id: Long) : RuntimeException("Refund not found: $id")

class RefundAlreadyRequestedException(cause: Throwable? = null) :
    RuntimeException("The booking already has a refund in progress or completed", cause)

class RefundWindowClosedException :
    RuntimeException("The flight leaves within 24 hours: only a SUPER_ADMIN can refund it, with override and a note")

class RefundOverrideNotAllowedException : RuntimeException("Only a SUPER_ADMIN can override the refund window")
