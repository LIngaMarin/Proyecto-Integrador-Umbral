package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.CalendarioService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.ZoneId

@RestController
class CalendarioController(private val service: CalendarioService,private val identidad: IdentidadAcademica) {
    /** GET: consulta el calendario del hijo vinculado; sin fecha inicial utiliza hoy en Lima. */
    @GetMapping("/api/apoderado/alumnos/{alumnoId}/simulacros")
    fun consultar(@AuthenticationPrincipal jwt: Jwt,@PathVariable alumnoId: Long,
        @RequestParam(required=false) desde: LocalDate?,@RequestParam(required=false) hasta: LocalDate?) =
        service.consultar(identidad.actor(jwt,"APODERADO"),alumnoId,
            desde ?: LocalDate.now(ZoneId.of("America/Lima")),hasta ?: LocalDate.of(9999,12,31))
}
