package com.dbook.infrastructure.security

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val authenticationEntryPoint: JsonAuthenticationEntryPoint,
    private val accessDeniedHandler: JsonAccessDeniedHandler,
) {
    /**
     * No frontend is deployed anywhere yet — every origin here is a local dev server
     * (Flutter web via `flutter run -d web-server`, on whatever port it picks). Revisit
     * with real origins once something is actually deployed.
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOriginPatterns = listOf("http://localhost:*", "http://127.0.0.1:*")
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("*")
                allowCredentials = true
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
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
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
                        // the public routes of the business API (version 1)
                        "/v1/auth/register",
                        "/v1/auth/login",
                        "/v1/auth/refresh",
                        "/v1/flights/search",
                        "/v1/flights/lowest-price",
                        "/v1/destinations",
                        "/v1/bookables/*/seats",
                    ).permitAll()
                    .anyRequest().authenticated()
            }.exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint)
                it.accessDeniedHandler(accessDeniedHandler)
            }.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
}
