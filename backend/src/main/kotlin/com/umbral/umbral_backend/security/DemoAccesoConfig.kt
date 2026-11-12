package com.umbral.umbral_backend.security

import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.*
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.bcrypt.BCrypt
import java.security.MessageDigest

/** Aísla las credenciales efímeras de las pruebas E2E; nunca se activa con PostgreSQL normal. */
@Configuration @Profile("demo")
class DemoAccesoConfig {
    @Bean fun accesoPruebasDemo(env:Environment,jdbc:JdbcTemplate)=ApplicationRunner {
        val password=env.getProperty("UMBRAL_DEMO_TEST_PASSWORD") ?: return@ApplicationRunner
        require(password.length in 16..128){"La contraseña efímera de pruebas debe tener entre 16 y 128 caracteres"}
        val digest=MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
        // Solo los tres fixtures demo; cada uno recibe una sal independiente y jamás se escribe texto plano.
        for(id in 1L..3L)jdbc.update("UPDATE auth_user SET password=? WHERE id=?","bcrypt_sha256$"+BCrypt.hashpw(digest,BCrypt.gensalt(12)),id)
    }
}
