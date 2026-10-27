package com.umbral.umbral_backend.controller

import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.http.*
import org.springframework.web.bind.annotation.*

@RestControllerAdvice
class ConflictoPersistenciaAdvice {
    @ExceptionHandler(DataIntegrityViolationException::class,PessimisticLockingFailureException::class)
    fun conflicto(): ResponseEntity<ProblemDetail> = ResponseEntity.status(409).body(
        ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Conflicto de persistencia; vuelva a consultar e intente de nuevo"))
}
