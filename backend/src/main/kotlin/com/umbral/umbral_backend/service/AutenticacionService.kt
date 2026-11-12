package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.security.*
import com.nimbusds.jose.*
import com.nimbusds.jose.crypto.MACSigner
import com.nimbusds.jwt.*
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.spec.SecretKeySpec

/** Emite acceso solo después de comprobar hash, usuario activo y rol académico en la base compartida. */
@Service
class AutenticacionService(private val jdbc: JdbcTemplate,private val identidad: IdentidadAcademica,
    private val passwords: PasswordAcademico,private val jwtKey: SecretKeySpec) {
    private data class Intentos(var cantidad: Int,val inicio: Instant)
    private val intentos=ConcurrentHashMap<String,Intentos>()
    // Hash de verificación ficticio: iguala el trabajo criptográfico cuando el usuario no existe.
    private val dummy="bcrypt_sha256\$\$2a\$12\$dQneMINJc2LNVbP3CMPR7u1CCA.ofGAHqMTbRmbMulqwrgAL08zbm"

    /** Limita intentos por dirección de conexión real; no confía en X-Forwarded-For enviado por visitantes. */
    private fun limitar(ip: String) {
        val now=Instant.now()
        intentos.entries.removeIf { it.value.inicio.plusSeconds(300).isBefore(now) }
        if(intentos.size>=10000 && !intentos.containsKey(ip)) throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Espera unos minutos antes de volver a intentar")
        val count=intentos.compute(ip) { _, previous ->
            (previous ?: Intentos(0,now)).also { it.cantidad++ }
        }!!
        if(count.cantidad>20) throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Demasiados intentos. Vuelve a intentarlo en cinco minutos")
    }

    fun iniciar(usuario: String,password: String,ip: String): Map<String,Any> {
        limitar(ip)
        val rows=identidad.consulta {
            jdbc.queryForList("""SELECT u.id,u.password,u.is_active,p.rol FROM auth_user u
                JOIN adm_perfil p ON p.user_id=u.id WHERE LOWER(u.email)=? OR LOWER(u.username)=?""",
                usuario.trim().lowercase(Locale.ROOT),usuario.trim().lowercase(Locale.ROOT))
                .map { row -> row.mapKeys { it.key.lowercase() } }
        }
        // Ambigüedad, usuario desactivado y contraseña errónea producen el mismo mensaje.
        val row=rows.singleOrNull()
        val valido=passwords.coincide(password,row?.get("password") as? String ?: dummy)
        if(row==null || !valido || row["is_active"]!=true || row["rol"] !in listOf("DOCENTE","ALUMNO","APODERADO"))
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED,"Usuario o contraseña incorrectos")
        val now=Instant.now()
        val claims=JWTClaimsSet.Builder().subject(row["id"].toString()).claim("user_id",row["id"].toString())
            .claim("token_type","access").issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(1800)))
            .jwtID(UUID.randomUUID().toString()).build()
        val jwt=SignedJWT(JWSHeader(JWSAlgorithm.HS256),claims).also { it.sign(MACSigner(jwtKey.encoded)) }
        return mapOf("accessToken" to jwt.serialize(),"expiresIn" to 1800,"rol" to row["rol"]!!)
    }
}
