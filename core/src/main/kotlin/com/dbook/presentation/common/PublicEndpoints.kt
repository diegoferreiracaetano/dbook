package com.dbook.presentation.common

/** An `/admin` controller whose endpoints are open on purpose (they create the session). */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class PublicEndpoints
