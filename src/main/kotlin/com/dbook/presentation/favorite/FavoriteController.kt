package com.dbook.presentation.favorite

import com.dbook.application.favorite.AddFavoriteUseCase
import com.dbook.application.favorite.ListFavoritesUseCase
import com.dbook.application.favorite.RemoveFavoriteUseCase
import com.dbook.domain.favorite.FavoriteType
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.PageParams
import com.dbook.presentation.common.PageResponse
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `/favorites` — the caller's own saved destinations and flights, kept on the server. */
@RestController
@RequestMapping("${ApiPaths.V1}/favorites")
@Tag(name = "Favorites", description = "The authenticated user's saved destinations and flights")
@SecurityRequirement(name = "bearerAuth")
class FavoriteController(
    private val addFavoriteUseCase: AddFavoriteUseCase,
    private val removeFavoriteUseCase: RemoveFavoriteUseCase,
    private val listFavoritesUseCase: ListFavoritesUseCase,
) {
    @Operation(summary = "The caller's favorites, newest first; `type` narrows to DESTINATION or FLIGHT")
    @GetMapping
    fun list(
        authentication: Authentication,
        @RequestParam(required = false) type: FavoriteType?,
        params: PageParams,
    ): PageResponse<FavoriteResponse> =
        PageResponse.from(
            listFavoritesUseCase.execute(authentication.currentUserId(), type, params.toQuery()),
            FavoriteResponse::from,
        )

    @Operation(
        summary = "Saves a destination (by IATA code) or a flight (by id); saving it again is fine",
        description = "404 when the target does not exist; 409 FAVORITES_LIMIT after 200 favorites.",
    )
    @PutMapping("/{type}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun add(
        @PathVariable type: FavoriteType,
        @PathVariable id: String,
        authentication: Authentication,
    ) = addFavoriteUseCase.execute(authentication.currentUserId(), type, id)

    @Operation(summary = "Removes a favorite; removing one that is not there is fine")
    @DeleteMapping("/{type}/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun remove(
        @PathVariable type: FavoriteType,
        @PathVariable id: String,
        authentication: Authentication,
    ) = removeFavoriteUseCase.execute(authentication.currentUserId(), type, id)
}
