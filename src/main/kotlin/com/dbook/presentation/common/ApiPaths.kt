package com.dbook.presentation.common

/**
 * Version prefixes of the public API. A controller of the business API is mapped under one of
 * these (`@RequestMapping("${ApiPaths.V1}/bookings")`); only the operational endpoints
 * (`/health`, `/ws`, the Swagger UI and the actuator) stay outside any version.
 *
 * Versions live in the presentation layer only: use cases and the domain do not know they exist,
 * so a future `/v2` is new controllers and DTOs calling the same use cases.
 */
object ApiPaths {
    const val V1 = "/v1"
}
