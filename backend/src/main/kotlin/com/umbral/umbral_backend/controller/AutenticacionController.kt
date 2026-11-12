package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.service.AutenticacionService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

/** No registra contraseñas ni las devuelve en respuestas; evita incluir el DTO en mensajes de depuración. */
class LoginDTO(val usuario: String,val password: String)

/** Acceso convencional; las APIs anteriores continúan aceptando sus access tokens habituales. */
@RestController
class AutenticacionController(private val service: AutenticacionService) {
    @PostMapping("/api/auth/login")
    fun login(@RequestBody dto: LoginDTO,request: HttpServletRequest): ResponseEntity<Map<String,Any>> {
        // La validación manual evita que los errores de Bean Validation impriman el valor de la contraseña.
        if(dto.usuario.isBlank() || dto.usuario.length>254 || dto.password.isBlank() || dto.password.length>128)
            throw org.springframework.web.server.ResponseStatusException(HttpStatus.BAD_REQUEST,"Completa tu usuario y contraseña")
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.iniciar(dto.usuario,dto.password,request.remoteAddr))
    }
}
