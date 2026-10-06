package com.dbook.infrastructure.security

import com.dbook.config.CorsProperties
import com.dbook.domain.common.access.Permission.ADMIN_PORTAL_ACCESS
import jakarta.servlet.DispatcherType
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.security.web.header.writers.StaticHeadersWriter
import org.springframework.security.web.util.matcher.AntPathRequestMatcher
import org.springframework.security.web.util.matcher.NegatedRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationEntryPoint: JsonAuthenticationEntryPoint,
    private val accessDeniedHandler: JsonAccessDeniedHandler,
    private val corsProperties: CorsProperties,
) {
    // patterns from cors.allowed-origins: today only local dev servers (Flutter web picks any port)
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
                allowCredentials = true
                allowedOriginPatterns = corsProperties.allowedOrigins
            }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .cors { it.configurationSource(corsConfigurationSource()) }
            .csrf { it.disable() }
            .headers { securityHeaders(it) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // A streamed response (the CSV export) is finished in an ASYNC dispatch, where the JWT filter does
                    // not run again and the stateless context is gone. The request was already authorized when it
                    // arrived (the controller's @PreAuthorize ran then); this dispatch only writes the body.
                    .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                    // Actuator (health probes, Prometheus) is served on its own management port,
                    // which is never exposed publicly. With a separate port this matcher only
                    // matches there — on the public port the same paths stay authenticated.
                    .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
                    .requestMatchers(
                        // operational: outside any API version
                        "/health",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        // browsers can't set Authorization on the WS handshake request;
                        // real auth happens on the STOMP CONNECT frame instead (5.7)
                        "/ws/**",
                        "/v1/ws/**",
                        // which app versions are served: asked before anyone signs in
                        "/v1/app-config",
                        "/v2/destinations",
                        // the public routes of the business API (version 1)
                        "/v1/auth/register",
                        "/v1/auth/login",
                        "/v1/auth/refresh",
                        "/v1/auth/verify-email",
                        "/v1/auth/forgot-password",
                        "/v1/auth/reset-password",
                        "/v1/admin/auth/login",
                        "/v1/admin/auth/refresh",
                        "/v1/admin/auth/logout",
                        "/v1/admin/auth/2fa/verify",
                        "/v1/admin/auth/2fa/enroll",
                        "/v1/admin/auth/2fa/confirm",
                        "/v1/admin/invitations/accept",
                        "/v1/flights/search",
                        "/v1/flights/lowest-price",
                        "/v1/flights/*/price-history",
                        "/v1/destinations",
                        "/v1/destinations/*/reviews",
                        "/v1/bookables/*/seats",
                    ).permitAll()
                    // reading hotels is public like reading flights; booking a stay (a POST) is not
                    .requestMatchers(HttpMethod.GET, "/v1/accommodations/**").permitAll()
                    .requestMatchers("/v1/admin/**").hasAuthority(ADMIN_PORTAL_ACCESS.name)
                    .anyRequest().authenticated()
            }.exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()

    // what every response carries: HTTPS only, no referrer, no sensor, and nothing loaded or framed
    private fun securityHeaders(headers: HeadersConfigurer<HttpSecurity>) {
        headers.httpStrictTransportSecurity { it.includeSubDomains(true).maxAgeInSeconds(HSTS_SECONDS) }
        headers.referrerPolicy { it.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER) }
        headers.addHeaderWriter(StaticHeadersWriter("Permissions-Policy", "geolocation=(), camera=(), microphone=()"))
        // an API serves data, never a page: nothing may be loaded or framed (Swagger UI is a page: left out)
        headers.addHeaderWriter(
            DelegatingRequestMatcherHeaderWriter(
                NegatedRequestMatcher(
                    OrRequestMatcher(AntPathRequestMatcher("/swagger-ui/**"), AntPathRequestMatcher("/v3/api-docs/**")),
                ),
                ContentSecurityPolicyHeaderWriter("default-src 'none'; frame-ancestors 'none'"),
            ),
        )
    }

    private companion object {
        const val HSTS_SECONDS = 31_536_000L
    }
}
