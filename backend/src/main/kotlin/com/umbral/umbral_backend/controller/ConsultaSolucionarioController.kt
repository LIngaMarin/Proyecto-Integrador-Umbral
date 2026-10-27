package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.ConsultaSolucionarioService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/practicas/preguntas")
class ConsultaSolucionarioController(private val service: ConsultaSolucionarioService, private val identidad: IdentidadAcademica) {
    @GetMapping("/{id}/solucionario")
    fun consultar(@AuthenticationPrincipal jwt: Jwt, @PathVariable id: Long) =
        service.consultar(identidad.actor(jwt,"ALUMNO"),id)
}
