package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.service.RespuestaPracticaService
import com.umbral.umbral_backend.security.IdentidadAcademica
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

data class RegistrarRespuestaDTO(@field:Positive val preguntaId: Long,
    @field:NotBlank @field:Size(max = 255) val respuesta: String)

@RestController
@RequestMapping("/api/practicas/respuestas")
class RespuestaPracticaController(private val service: RespuestaPracticaService, private val identidad: IdentidadAcademica) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody dto: RegistrarRespuestaDTO) =
        service.registrar(identidad.actor(jwt, "ALUMNO"), dto.preguntaId, dto.respuesta)
}
