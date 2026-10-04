package com.dbook.domain.identity

// only thrown after the password was right, so it never reveals which accounts exist
class AccountBlockedException : RuntimeException("This account is blocked")
