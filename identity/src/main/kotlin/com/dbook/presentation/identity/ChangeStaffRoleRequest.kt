package com.dbook.presentation.identity

import com.dbook.domain.common.access.Role
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

// single-parameter data class: needs an explicit creator, see RefreshRequest/SuggestFlightsRequest
data class ChangeStaffRoleRequest
    @JsonCreator
    constructor(
        @get:Schema(example = "CATALOG_MANAGER")
        @JsonProperty("role")
        val role: Role,
    )
