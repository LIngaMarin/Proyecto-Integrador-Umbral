package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.dto.PreguntaPracticaDTO
import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.service.BancoPreguntasService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** Consulta del catálogo aprobado; no permite leer soluciones ni registrar intentos. */
@RestController
@RequestMapping("/api/practicas")
class PracticaController(private val bancoService: BancoPreguntasService) {
    @GetMapping("/preguntas")
    fun preguntas(
        @RequestParam tema: String,
        @RequestParam dificultad: Dificultad
    ): List<PreguntaPracticaDTO> = bancoService.obtenerPreguntasPractica(tema, dificultad)
}
