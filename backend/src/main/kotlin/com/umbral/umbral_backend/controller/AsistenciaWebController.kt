package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.AsistenciaWebService
import jakarta.validation.Valid
import jakarta.validation.constraints.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

/** Datos de una clase; evita títulos vacíos, identificadores inválidos y fechas implícitas. */
data class CrearClaseDTO(@field:Positive val seccionId: Long,
    @field:NotBlank @field:Size(max=150) val titulo: String,val fin: LocalDateTime)

/** Todas las operaciones comprueban rol e identidad en la base compartida. */
@RestController
class AsistenciaWebController(private val service: AsistenciaWebService, private val identidad: IdentidadAcademica) {
    @GetMapping("/api/docente/clases")
    fun clases(@AuthenticationPrincipal jwt: Jwt) = service.clases(identidad.actor(jwt,"DOCENTE"))
    @PostMapping("/api/docente/clases") @ResponseStatus(HttpStatus.CREATED)
    fun crear(@AuthenticationPrincipal jwt: Jwt,@Valid @RequestBody dto: CrearClaseDTO) =
        service.crear(identidad.actor(jwt,"DOCENTE"),dto.seccionId,dto.titulo,dto.fin)
    @GetMapping("/api/docente/clases/{id}/alumnos")
    fun alumnos(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long) = service.alumnos(identidad.actor(jwt,"DOCENTE"),id)
    @PutMapping("/api/docente/clases/{id}/presentes/{alumnoId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun presente(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long,@PathVariable alumnoId: Long) =
        service.presente(identidad.actor(jwt,"DOCENTE"),id,alumnoId)
    @PutMapping("/api/docente/clases/{id}/cerrar") @ResponseStatus(HttpStatus.NO_CONTENT)
    fun cerrar(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long) = service.cerrar(identidad.actor(jwt,"DOCENTE"),id)
    @GetMapping("/api/apoderado/alumnos/{alumnoId}/notificaciones")
    fun notificaciones(@AuthenticationPrincipal jwt: Jwt,@PathVariable alumnoId: Long) =
        service.notificaciones(identidad.actor(jwt,"APODERADO"),alumnoId)
}
