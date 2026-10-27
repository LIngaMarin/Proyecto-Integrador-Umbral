package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.RespuestaGrupoService
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

data class ResponderGrupoDTO(@field:NotBlank @field:Size(max=10000) val texto: String)

@RestController
class RespuestaGrupoController(private val service: RespuestaGrupoService,private val identidad: IdentidadAcademica) {
    @PostMapping("/api/docente/dudas/grupos/{id}/respuestas") @ResponseStatus(HttpStatus.CREATED)
    fun responder(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long,@Valid @RequestBody dto: ResponderGrupoDTO) =
        service.responder(identidad.actor(jwt,"DOCENTE"),id,dto.texto)
    @GetMapping("/api/docente/dudas/grupos/{id}/respuestas")
    fun historial(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long) = service.historial(identidad.actor(jwt,"DOCENTE"),id)
    @GetMapping("/api/alumno/dudas/{id}/respuesta")
    fun consultar(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long) = service.consultar(identidad.actor(jwt,"ALUMNO"),id)
}
