package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.ResolucionDudaService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
class ResolucionDudaController(private val service: ResolucionDudaService,private val identidad: IdentidadAcademica) {
    /** PUT: solicita la resolución de una duda como alumno propietario. */
    @PutMapping("/api/alumno/dudas/{id}/resolver")
    fun alumno(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long)=service.resolver(identidad.actor(jwt,"ALUMNO"),id)
    /** PUT: solicita la resolución de una duda como docente asignado. */
    @PutMapping("/api/docente/dudas/{id}/resolver")
    fun docente(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long)=service.resolver(identidad.actor(jwt,"DOCENTE"),id)
    /** GET: consulta el historial de una duda propia del alumno. */
    @GetMapping("/api/alumno/dudas/{id}/historial")
    fun historialAlumno(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long)=service.consultarHistorial(identidad.actor(jwt,"ALUMNO"),id)
    /** GET: consulta el historial de una duda autorizada para el docente. */
    @GetMapping("/api/docente/dudas/{id}/historial")
    fun historialDocente(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long)=service.consultarHistorial(identidad.actor(jwt,"DOCENTE"),id)
}
