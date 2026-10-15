package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.AprobarRechazarDTO
import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.model.EstadoPregunta
import com.umbral.umbral_backend.model.Pregunta
import com.umbral.umbral_backend.repository.PreguntaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class BancoPreguntasServiceHu21Tests {
    private val repository = Mockito.mock(PreguntaRepository::class.java)
    private val service = BancoPreguntasService(repository)

    @Test
    fun `rechazar guarda estado y motivo sin espacios externos`() {
        val pregunta = pregunta()
        `when`(repository.findById(7L)).thenReturn(Optional.of(pregunta))
        `when`(repository.save(pregunta)).thenReturn(pregunta)

        val resultado = service.rechazarPregunta(7L, AprobarRechazarDTO("  Motivo válido  "))

        assertEquals(EstadoPregunta.RECHAZADA, resultado.estado)
        assertEquals("Motivo válido", resultado.motivoRechazo)
        Mockito.verify(repository).save(pregunta)
    }

    @Test
    fun `rechazar requiere un motivo no vacio`() {
        assertThrows(IllegalArgumentException::class.java) {
            service.rechazarPregunta(7L, AprobarRechazarDTO("   "))
        }
        Mockito.verifyNoInteractions(repository)
    }

    @Test
    fun `rechazar devuelve 404 si la pregunta no existe`() {
        `when`(repository.findById(99L)).thenReturn(Optional.empty())

        val error = assertThrows(ResponseStatusException::class.java) {
            service.rechazarPregunta(99L, AprobarRechazarDTO("Motivo válido"))
        }

        assertEquals(404, error.statusCode.value())
    }

    private fun pregunta() = Pregunta(
        enunciado = "Pregunta de prueba",
        tema = "Álgebra",
        dificultad = Dificultad.FACIL,
        opciones = listOf("A", "B"),
        respuestaCorrecta = "A",
        docenteId = 1L
    )
}
