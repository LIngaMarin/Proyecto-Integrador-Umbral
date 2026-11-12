package com.umbral.umbral_backend.controller

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestControllerAdvice
class ConflictoPersistenciaAdvice {
    /** Comunica reglas de negocio sin trazas, valores rechazados ni información de credenciales. */
    @ExceptionHandler(org.springframework.web.server.ResponseStatusException::class)
    fun regla(ex:org.springframework.web.server.ResponseStatusException):ResponseEntity<ProblemDetail> =
        ResponseEntity.status(ex.statusCode).cacheControl(CacheControl.noStore()).body(
            ProblemDetail.forStatusAndDetail(ex.statusCode,ex.reason ?: "No se puede realizar esta operación"))

    /** Devuelve 409 ante conflictos de integridad o bloqueo sin exponer detalles de la base de datos. */
    @ExceptionHandler(DataIntegrityViolationException::class,PessimisticLockingFailureException::class)
    fun conflicto(): ResponseEntity<ProblemDetail> = ResponseEntity.status(409).body(
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Conflicto de persistencia; vuelva a consultar e intente de nuevo"))
}
