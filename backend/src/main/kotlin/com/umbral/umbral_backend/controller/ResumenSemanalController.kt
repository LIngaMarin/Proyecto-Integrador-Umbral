package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.EstadisticasSemanaService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
class ResumenSemanalController(private val service: EstadisticasSemanaService,private val identidad: IdentidadAcademica) {
    /** GET: consulta las estadísticas del docente; sin semana indicada utiliza la anterior. */
    @GetMapping("/api/docente/resumen-semanal")
    fun consultar(@AuthenticationPrincipal jwt: Jwt,@RequestParam(required=false) semana: LocalDate?) =
        service.calcular(identidad.actor(jwt,"DOCENTE"),semana ?: service.semanaAnterior())
}
