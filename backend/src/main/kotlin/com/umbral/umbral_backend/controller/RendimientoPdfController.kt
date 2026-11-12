package com.umbral.umbral_backend.controller

import com.umbral.umbral_backend.security.IdentidadAcademica
import com.umbral.umbral_backend.service.RendimientoPdfService
import org.springframework.http.*
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
class RendimientoPdfController(private val service: RendimientoPdfService,private val identidad: IdentidadAcademica) {
    /** HU41: consulta previa y filtros disponibles, con idénticos permisos y cálculos que la exportación. */
    @GetMapping("/api/docente/secciones/{id}/rendimiento")
    fun consultar(@AuthenticationPrincipal jwt:Jwt,@PathVariable id:Long,
        @RequestParam(required=false) simulacroId:Long?) = ResponseEntity.ok().cacheControl(CacheControl.noStore())
            .body(service.consultar(identidad.actor(jwt,"DOCENTE"),id,simulacroId))

    /** GET: descarga el PDF de una sección autorizada e indica que no debe almacenarse en caché. */
    @GetMapping("/api/docente/secciones/{id}/rendimiento.pdf",produces=[MediaType.APPLICATION_PDF_VALUE])
    fun descargar(@AuthenticationPrincipal jwt: Jwt,@PathVariable id: Long,
        @RequestParam(required=false) simulacroId:Long?): ResponseEntity<ByteArray> {
        val contenido=service.generar(identidad.actor(jwt,"DOCENTE"),id,simulacroId)
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"rendimiento-seccion-$id.pdf\"")
            .header("X-Content-Type-Options","nosniff")
            .contentLength(contenido.size.toLong())
            .cacheControl(CacheControl.noStore()).body(contenido)
    }
}
