package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.dto.PreguntaPracticaDTO
import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.BancoPreguntasService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Catálogo autenticado de práctica: restringe las preguntas al contexto académico del alumno. */
@RestController
@RequestMapping("/api/alumno/practicas")
class PracticaAlumnoController(
    private val bancoService: BancoPreguntasService,
    private val identidad: IdentidadAcademica
) {
    @GetMapping("/preguntas")
    fun preguntas(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam tema: String,
        @RequestParam dificultad: Dificultad
    ): List<PreguntaPracticaDTO> {
        val alumno = identidad.actor(jwt, "ALUMNO")
        val docentes = identidad.docentesDisponiblesParaPractica(alumno.id)
        return bancoService.obtenerPreguntasPracticaDeAlumno(tema, dificultad, docentes)
    }
}