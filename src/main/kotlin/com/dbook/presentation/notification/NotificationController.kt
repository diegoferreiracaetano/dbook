package com.dbook.presentation.notification

import com.dbook.application.notification.CountUnreadNotificationsUseCase
import com.dbook.application.notification.GetNotificationPreferencesUseCase
import com.dbook.application.notification.ListNotificationsUseCase
import com.dbook.application.notification.MarkAllNotificationsReadUseCase
import com.dbook.application.notification.MarkNotificationReadUseCase
import com.dbook.application.notification.RegisterDeviceUseCase
import com.dbook.application.notification.UnregisterDeviceUseCase
import com.dbook.application.notification.UpdateNotificationPreferencesUseCase
import com.dbook.presentation.common.ApiPaths
import com.dbook.presentation.common.CursorPageResponse
import com.dbook.presentation.common.currentUserId
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** `/notifications` — the caller's own inbox, preferences and devices; never anyone else's. */
@RestController
@RequestMapping("${ApiPaths.V1}/notifications")
@Tag(name = "Notifications", description = "The authenticated user's inbox, preferences and devices")
@SecurityRequirement(name = "bearerAuth")
class NotificationController(
    private val listNotificationsUseCase: ListNotificationsUseCase,
    private val countUnreadNotificationsUseCase: CountUnreadNotificationsUseCase,
    private val markNotificationReadUseCase: MarkNotificationReadUseCase,
    private val markAllNotificationsReadUseCase: MarkAllNotificationsReadUseCase,
    private val getNotificationPreferencesUseCase: GetNotificationPreferencesUseCase,
    private val updateNotificationPreferencesUseCase: UpdateNotificationPreferencesUseCase,
    private val registerDeviceUseCase: RegisterDeviceUseCase,
    private val unregisterDeviceUseCase: UnregisterDeviceUseCase,
) {
    @Operation(summary = "The inbox, newest first; send the `nextCursor` back as `cursor` for the next page")
    @GetMapping
    fun list(
        authentication: Authentication,
        @RequestParam(required = false) cursor: Long?,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(defaultValue = "false") unreadOnly: Boolean,
    ): CursorPageResponse<NotificationResponse> {
        val page = listNotificationsUseCase.execute(authentication.currentUserId(), cursor, size, unreadOnly)
        return CursorPageResponse(page.items.map(NotificationResponse::from), page.next?.toString())
    }

    @Operation(summary = "How many notifications are unread, for the badge")
    @GetMapping("/unread-count")
    fun unreadCount(authentication: Authentication) =
        UnreadCountResponse(countUnreadNotificationsUseCase.execute(authentication.currentUserId()))

    @Operation(summary = "Marks one notification as read (404 if it is not yours)")
    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markRead(
        @PathVariable id: Long,
        authentication: Authentication,
    ) = markNotificationReadUseCase.execute(authentication.currentUserId(), id)

    @Operation(summary = "Marks every notification as read")
    @PostMapping("/read-all")
    fun markAllRead(authentication: Authentication) =
        MarkedReadResponse(markAllNotificationsReadUseCase.execute(authentication.currentUserId()))

    @Operation(summary = "What you get, on which channel, for every type (enabled unless you turned it off)")
    @GetMapping("/preferences")
    fun preferences(authentication: Authentication): List<PreferenceDto> =
        getNotificationPreferencesUseCase.execute(authentication.currentUserId()).map(PreferenceDto::from)

    @Operation(summary = "Turns channels on or off per type; only the pairs sent change. Returns what is now in force")
    @PutMapping("/preferences")
    fun updatePreferences(
        @RequestBody changes: List<PreferenceDto>,
        authentication: Authentication,
    ): List<PreferenceDto> =
        updateNotificationPreferencesUseCase.execute(
            authentication.currentUserId(),
            changes.map(PreferenceDto::toDomain),
        )
            .map(PreferenceDto::from)

    @Operation(summary = "Registers this device for push (registering the same token again just refreshes it)")
    @PostMapping("/devices")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun registerDevice(
        @RequestBody request: RegisterDeviceRequest,
        authentication: Authentication,
    ) = registerDeviceUseCase.execute(authentication.currentUserId(), request.token, request.platform)

    @Operation(summary = "Stops sending push to this device (for instance when the user signs out)")
    @DeleteMapping("/devices/{token}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unregisterDevice(
        @PathVariable token: String,
        authentication: Authentication,
    ) = unregisterDeviceUseCase.execute(authentication.currentUserId(), token)
}
