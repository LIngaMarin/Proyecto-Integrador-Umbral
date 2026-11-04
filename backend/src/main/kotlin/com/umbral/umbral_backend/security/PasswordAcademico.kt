package com.umbral.umbral_backend.security

import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Verifica formatos existentes de Django sin copiar ni modificar sus contraseñas. */
@Component
class PasswordAcademico {
    /** BCrypt-SHA256 evita el truncamiento a 72 bytes; PBKDF2 conserva acceso a usuarios administrativos existentes. */
    fun coincide(password: String, encoded: String): Boolean = try {
        when {
            encoded.startsWith("bcrypt_sha256$") -> {
                val digest=MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
                    .joinToString("") { "%02x".format(it) }
                BCrypt.checkpw(digest,encoded.removePrefix("bcrypt_sha256$"))
            }
            encoded.startsWith("pbkdf2_sha256$") -> {
                val parts=encoded.split('$')
                val iterations=parts.getOrNull(1)?.toIntOrNull() ?: 0
                // Los límites impiden trabajo descontrolado ante hashes corruptos o manipulados.
                if(parts.size!=4 || iterations !in 10000..5000000 || parts[2].length !in 1..128) false
                else {
                    val expected=Base64.getDecoder().decode(parts[3])
                    if(expected.size!=32) false else {
                        val spec=PBEKeySpec(password.toCharArray(),parts[2].toByteArray(Charsets.UTF_8),iterations,256)
                        try { MessageDigest.isEqual(expected,SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded) }
                        finally { spec.clearPassword() }
                    }
                }
            }
            else -> false // Contraseñas inutilizables o algoritmos no admitidos nunca conceden acceso.
        }
    } catch (_: IllegalArgumentException) { false }
}
