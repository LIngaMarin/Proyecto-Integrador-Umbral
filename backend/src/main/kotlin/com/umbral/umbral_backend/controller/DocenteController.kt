package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.dto.ActualizarPreguntaDTO
import com.umbral.umbral_backend.dto.AprobarRechazarDTO
import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.model.Pregunta
import com.umbral.umbral_backend.model.Solucionario
import com.umbral.umbral_backend.service.BancoPreguntasService
import com.umbral.umbral_backend.service.SolucionarioService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/docente")
@CrossOrigin(origins = ["*"])
class DocenteController(
    private val bancoService: BancoPreguntasService,
    private val solucionarioService: SolucionarioService
) {
    @GetMapping("/preguntas")
    fun listarPreguntas(
        @RequestParam docenteId: Long,
        @RequestParam(required = false) tema: String?,
        @RequestParam(required = false) dificultad: Dificultad?
    ): ResponseEntity<List<Pregunta>> =
        ResponseEntity.ok(bancoService.obtenerPreguntas(docenteId, tema, dificultad))

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
    @PutMapping("/preguntas/{id}/aprobar")
    fun aprobarPregunta(
        @PathVariable id: Long,
        @RequestBody dto: ActualizarPreguntaDTO
    ): ResponseEntity<Pregunta> =
        ResponseEntity.ok(bancoService.editarYAprobar(id, dto))

    @PutMapping("/preguntas/{id}/rechazar")
    fun rechazarPregunta(
        @PathVariable id: Long,
        @Valid @RequestBody dto: AprobarRechazarDTO
    ): ResponseEntity<Pregunta> =
        ResponseEntity.ok(bancoService.rechazarPregunta(id, dto))

    @PostMapping("/preguntas/{id}/solucionario")
    fun generarSolucionario(@PathVariable id: Long): ResponseEntity<Solucionario> =
        ResponseEntity.ok(solucionarioService.generarParaPregunta(id))
}
