package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.dto.RegistrarDudaDTO
import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.DudaService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
class DudaController(private val service: DudaService, private val identidad: IdentidadAcademica) {
    @PostMapping("/api/alumno/dudas") @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@AuthenticationPrincipal jwt: Jwt,@Valid @RequestBody dto: RegistrarDudaDTO) =
        service.registrar(identidad.actor(jwt,"ALUMNO"),dto)
    @GetMapping("/api/alumno/dudas")
    fun propias(@AuthenticationPrincipal jwt: Jwt) = service.propias(identidad.actor(jwt,"ALUMNO"))
    @GetMapping("/api/docente/dudas/grupos")
    fun grupos(@AuthenticationPrincipal jwt: Jwt) = service.agrupaciones(identidad.actor(jwt,"DOCENTE"))
    @GetMapping("/api/docente/dudas/grupos/{id}")
    fun detalle(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long) = service.delGrupo(identidad.actor(jwt,"DOCENTE"),id)
}
