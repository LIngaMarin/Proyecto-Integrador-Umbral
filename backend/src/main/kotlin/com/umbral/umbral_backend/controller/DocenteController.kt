package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.model.Pregunta
import com.umbral.umbral_backend.service.BancoPreguntasService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/docente")
@CrossOrigin(origins = ["*"])
class DocenteController(
    private val bancoService: BancoPreguntasService
) {
    @PostMapping("/boletin/extraer")
    fun extraerBoletin(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("docenteId") docenteId: Long
    ): ResponseEntity<List<Pregunta>> =
        ResponseEntity.ok(bancoService.extraerPreguntasDeBoletin(file, docenteId))
    @PostMapping("/preguntas/{id}/variantes")
    fun generarVariante(
        @PathVariable id: Long,
        @RequestParam docenteId: Long
    ): ResponseEntity<Pregunta> =
        ResponseEntity.ok(bancoService.generarVariante(id, docenteId))
}