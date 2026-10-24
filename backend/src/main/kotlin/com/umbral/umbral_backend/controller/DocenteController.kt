package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.dto.ActualizarPreguntaDTO
import com.umbral.umbral_backend.dto.AprobarRechazarDTO
import com.umbral.umbral_backend.dto.TemaDebilDTO
import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.model.Pregunta
import com.umbral.umbral_backend.model.Solucionario
import com.umbral.umbral_backend.service.BancoPreguntasService
import com.umbral.umbral_backend.service.SolucionarioService
import com.umbral.umbral_backend.service.SimulacroService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

/**
 * Recibe las solicitudes de la API docente y delega el trabajo a los servicios.
 * @RestController expone los métodos como endpoints REST y @RequestMapping fija el prefijo común.
 * @CrossOrigin permite solicitudes desde cualquier origen según la configuración existente.
 */
@RestController
@RequestMapping("/api/docente")
@CrossOrigin(origins = ["*"])
class DocenteController(
    private val bancoService: BancoPreguntasService,
    private val solucionarioService: SolucionarioService,
    private val simulacroService: SimulacroService
) {
    /** GET /preguntas: docenteId es obligatorio; tema y dificultad son filtros opcionales delegados al servicio. */
    @GetMapping("/preguntas")
    fun listarPreguntas(
        @RequestParam docenteId: Long,
        @RequestParam(required = false) tema: String?,
        @RequestParam(required = false) dificultad: Dificultad?
    ): ResponseEntity<List<Pregunta>> =
        ResponseEntity.ok(bancoService.obtenerPreguntas(docenteId, tema, dificultad))

    /** POST /boletin/extraer: recibe file y docenteId como parámetros y delega la operación al servicio. */
    @PostMapping("/boletin/extraer")
    fun extraerBoletin(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("docenteId") docenteId: Long
    ): ResponseEntity<List<Pregunta>> =
        ResponseEntity.ok(bancoService.extraerPreguntasDeBoletin(file, docenteId))
    /** POST /preguntas/{id}/variantes: toma el id de ruta y docenteId de consulta para delegar la variante. */
    @PostMapping("/preguntas/{id}/variantes")
    fun generarVariante(
        @PathVariable id: Long,
        @RequestParam docenteId: Long
    ): ResponseEntity<Pregunta> =
        ResponseEntity.ok(bancoService.generarVariante(id, docenteId))
    /** PUT /preguntas/{id}/aprobar: toma el id de ruta y el DTO del cuerpo para delegar edición y aprobación. */
    @PutMapping("/preguntas/{id}/aprobar")
    fun aprobarPregunta(
        @PathVariable id: Long,
        @RequestBody dto: ActualizarPreguntaDTO
    ): ResponseEntity<Pregunta> =
        ResponseEntity.ok(bancoService.editarYAprobar(id, dto))

    /** PUT /preguntas/{id}/rechazar: toma el id de ruta y valida el DTO del cuerpo antes de delegar el rechazo. */
    @PutMapping("/preguntas/{id}/rechazar")
    fun rechazarPregunta(
        @PathVariable id: Long,
        @Valid @RequestBody dto: AprobarRechazarDTO
    ): ResponseEntity<Pregunta> =
        ResponseEntity.ok(bancoService.rechazarPregunta(id, dto))

    /** POST /preguntas/{id}/solucionario: usa el id de ruta para delegar la generación y guardado. */
    @PostMapping("/preguntas/{id}/solucionario")
    fun generarSolucionario(@PathVariable id: Long): ResponseEntity<Solucionario> =
        ResponseEntity.ok(solucionarioService.generarParaPregunta(id))

    /** PUT /solucionarios/{id}/aprobar: usa el id de ruta para delegar el cambio de estado. */
    @PutMapping("/solucionarios/{id}/aprobar")
    fun aprobarSolucionario(@PathVariable id: Long): ResponseEntity<Solucionario> =
        ResponseEntity.ok(solucionarioService.aprobar(id))

    /** GET /secciones/{seccionId}/temas-debiles: usa el id de sección de la ruta y delega su cálculo. */
    @GetMapping("/secciones/{seccionId}/temas-debiles")
    fun obtenerTemasDebiles(@PathVariable seccionId: Long): ResponseEntity<List<TemaDebilDTO>> =
        ResponseEntity.ok(simulacroService.calcularTemasDebiles(seccionId))
}
