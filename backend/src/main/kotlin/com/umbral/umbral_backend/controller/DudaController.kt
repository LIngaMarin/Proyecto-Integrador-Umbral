package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.dto.RegistrarDudaDTO
import com.umbral.umbral_backend.model.EstadoDuda
import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.DudaService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
class DudaController(private val service: DudaService, private val identidad: IdentidadAcademica) {
    /** POST /api/alumno/dudas: valida los datos y registra la duda del alumno autenticado. */
    @PostMapping("/api/alumno/dudas") @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@AuthenticationPrincipal jwt: Jwt,@Valid @RequestBody dto: RegistrarDudaDTO) =
        service.registrar(identidad.actor(jwt,"ALUMNO"),dto)
    /** GET /api/alumno/dudas: consulta las dudas propias con filtro opcional de estado. */
    @GetMapping("/api/alumno/dudas")
    fun propias(@AuthenticationPrincipal jwt: Jwt,@RequestParam(required=false) estado: EstadoDuda?) =
        service.propias(identidad.actor(jwt,"ALUMNO"),estado)
    /** GET /api/docente/dudas/grupos: consulta las agrupaciones autorizadas para el docente. */
    @GetMapping("/api/docente/dudas/grupos")
    fun grupos(@AuthenticationPrincipal jwt: Jwt) = service.agrupaciones(identidad.actor(jwt,"DOCENTE"))
    /** GET /api/docente/dudas/grupos/{id}: consulta las dudas del grupo y delega los permisos. */
    @GetMapping("/api/docente/dudas/grupos/{id}")
    fun detalle(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long,@RequestParam(required=false) estado: EstadoDuda?) =
        service.delGrupo(identidad.actor(jwt,"DOCENTE"),id,estado)
}
