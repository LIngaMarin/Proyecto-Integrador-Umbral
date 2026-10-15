package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.model.EstadoSolucionario
import com.umbral.umbral_backend.model.Pregunta
import com.umbral.umbral_backend.model.Solucionario
import com.umbral.umbral_backend.repository.PreguntaRepository
import com.umbral.umbral_backend.repository.SolucionarioRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class SolucionarioServiceHu26Tests {
    private val preguntaRepository = Mockito.mock(PreguntaRepository::class.java)
    private val solucionarioRepository = Mockito.mock(SolucionarioRepository::class.java)
    private val service = SolucionarioService(preguntaRepository, solucionarioRepository)

    /** Comprueba que el borrador cambia a aprobado y el cambio se guarda. */
    @Test
    fun `aprueba y persiste un solucionario en borrador`() {
        val solucionario = solucionario(EstadoSolucionario.BORRADOR)
        `when`(solucionarioRepository.findById(5L)).thenReturn(Optional.of(solucionario))
        `when`(solucionarioRepository.save(solucionario)).thenReturn(solucionario)

        val resultado = service.aprobar(5L)

        assertEquals(EstadoSolucionario.APROBADO, resultado.estado)
        Mockito.verify(solucionarioRepository).save(solucionario)
    }

    /** Comprueba el 404 y la ausencia de guardado cuando no existe el solucionario. */
    @Test
    fun `retorna 404 si el solucionario no existe`() {
        `when`(solucionarioRepository.findById(99L)).thenReturn(Optional.empty())

        val error = assertThrows(ResponseStatusException::class.java) {
            service.aprobar(99L)
        }

        assertEquals(404, error.statusCode.value())
        Mockito.verify(solucionarioRepository, Mockito.never()).save(Mockito.any(Solucionario::class.java))
    }

    /** Comprueba que un solucionario ya aprobado no atraviesa otra vez la transición. */
    @Test
    fun `rechaza una transicion si el solucionario ya esta aprobado`() {
        val solucionario = solucionario(EstadoSolucionario.APROBADO)
        `when`(solucionarioRepository.findById(5L)).thenReturn(Optional.of(solucionario))

        val error = assertThrows(ResponseStatusException::class.java) {
            service.aprobar(5L)
        }

        assertEquals(409, error.statusCode.value())
        Mockito.verify(solucionarioRepository, Mockito.never()).save(Mockito.any(Solucionario::class.java))
    }

    private fun solucionario(estado: EstadoSolucionario) = Solucionario(
        id = 5L,
        pregunta = Pregunta(
            id = 10L,
            enunciado = "Question",
            tema = "Algebra",
            dificultad = Dificultad.FACIL,
            opciones = listOf("2", "4"),
            respuestaCorrecta = "4",
            docenteId = 3L
        ),
        pasoAPaso = "Verified steps",
        estado = estado
    )
}
