package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.repository.*
import com.umbral.umbral_backend.security.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*
import org.springframework.web.server.ResponseStatusException

/** Verifica que las respuestas inválidas se rechacen antes de consultar o guardar datos. */
class RespuestaPracticaUnitTests {
    private val preguntas = mock(PreguntaRepository::class.java)
    private val respuestas = mock(RespuestaPracticaRepository::class.java)
    private val identidad = mock(IdentidadAcademica::class.java)
    private val service = RespuestaPracticaService(preguntas,respuestas,identidad)

    @Test fun `rechaza solicitudes invalidas sin persistir`() {
        for ((id, respuesta) in listOf(0L to "4",1L to " ",1L to "a".repeat(256))) {
            assertEquals(400, assertThrows(ResponseStatusException::class.java) {
                service.registrar(Actor(2,"ALUMNO",""),id,respuesta)
            }.statusCode.value())
        }
        verifyNoInteractions(preguntas,respuestas,identidad)
    }
}
