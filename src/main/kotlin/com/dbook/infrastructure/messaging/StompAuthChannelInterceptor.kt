package com.dbook.infrastructure.messaging

import com.dbook.domain.TokenService
import org.springframework.messaging.Message
import org.springframework.messaging.MessageChannel
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.ChannelInterceptor
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component

// A browser's native WebSocket API can't set an Authorization header on the handshake
// HTTP request, so /ws is public in SecurityConfig — this is where auth actually
// happens instead, on the STOMP CONNECT frame itself (which carries arbitrary headers).
@Component
class StompAuthChannelInterceptor(
    private val tokenService: TokenService,
) : ChannelInterceptor {
    override fun preSend(
        message: Message<*>,
        channel: MessageChannel,
    ): Message<*> {
        val accessor = StompHeaderAccessor.wrap(message)
        if (accessor.command == StompCommand.CONNECT) {
            val token = accessor.getFirstNativeHeader("Authorization")?.removePrefix("Bearer ")
            val userId = token?.let(tokenService::parseUserId)
            val role = token?.let(tokenService::parseRole)
            if (userId == null || role == null) {
                throw BadCredentialsException("Missing or invalid token on STOMP CONNECT")
            }
            accessor.user =
                UsernamePasswordAuthenticationToken(
                    userId.toString(),
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_$role")),
                )
        }
        return message
    }
}
