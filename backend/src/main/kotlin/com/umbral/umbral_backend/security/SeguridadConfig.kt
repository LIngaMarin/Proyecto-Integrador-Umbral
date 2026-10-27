package com.umbral.umbral_backend.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.*
import org.springframework.security.oauth2.core.*
import org.springframework.security.web.SecurityFilterChain
import java.security.SecureRandom
import javax.crypto.spec.SecretKeySpec

/** Compatible con access tokens HS256 de Django SimpleJWT. Nunca acepta refresh tokens. */
@Configuration
class SeguridadConfig {
    @Bean
    fun jwtDecoder(env: Environment): JwtDecoder {
        val configured = env.getProperty("umbral.jwt.secret", env.getProperty("UMBRAL_JWT_SECRET", ""))
        // Sin secreto compartido configurado, ningún token externo será aceptado.
        val key = if (configured.isBlank()) ByteArray(64).also { SecureRandom().nextBytes(it) }
            else configured.toByteArray(Charsets.UTF_8)
        require(key.size >= 32) { "UMBRAL_JWT_SECRET debe tener al menos 32 bytes" }
        return NimbusJwtDecoder.withSecretKey(SecretKeySpec(key, "HmacSHA256")).build().also {
            it.setJwtValidator(DelegatingOAuth2TokenValidator(JwtValidators.createDefault(), OAuth2TokenValidator<Jwt> { jwt ->
                if (jwt.getClaimAsString("token_type") == "access" && jwt.expiresAt != null &&
                    jwt.getClaimAsString("user_id")?.toLongOrNull()?.let { id -> id > 0 } == true)
                    OAuth2TokenValidatorResult.success()
                else OAuth2TokenValidatorResult.failure(OAuth2Error("invalid_token"))
            }))
        }
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/api/practicas/preguntas").permitAll()
                    .anyRequest().authenticated()
            }
            .oauth2ResourceServer { it.jwt { } }
        return http.build()
    }
}
