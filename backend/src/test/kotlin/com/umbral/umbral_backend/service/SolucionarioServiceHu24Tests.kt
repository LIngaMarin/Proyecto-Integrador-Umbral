package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.model.EstadoSolucionario
import com.umbral.umbral_backend.model.Pregunta
import com.umbral.umbral_backend.model.Solucionario
import com.umbral.umbral_backend.repository.PreguntaRepository
import com.umbral.umbral_backend.repository.SolucionarioRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class SolucionarioServiceHu24Tests {
    private val preguntaRepository = Mockito.mock(PreguntaRepository::class.java)
    private val solucionarioRepository = Mockito.mock(SolucionarioRepository::class.java)
    private val service = SolucionarioService(preguntaRepository, solucionarioRepository)

    /** Comprueba la generación de pasos de ejemplo y el guardado en borrador. */
    @Test
    fun `genera y guarda una solucion verificada como borrador`() {
        val pregunta = pregunta()
        `when`(preguntaRepository.findById(42L)).thenReturn(Optional.of(pregunta))
        `when`(solucionarioRepository.existsByPregunta_Id(42L)).thenReturn(false)
        `when`(solucionarioRepository.save(Mockito.any(Solucionario::class.java)))
            .thenAnswer { invocation -> invocation.getArgument(0) }

        val resultado = service.generarParaPregunta(42L)

        assertEquals(EstadoSolucionario.BORRADOR, resultado.estado)
        assertEquals(pregunta, resultado.pregunta)
        assertTrue(resultado.pasoAPaso.contains("2x + 6 = 14"))
        assertTrue(resultado.pasoAPaso.contains("2x = 8"))
        assertTrue(resultado.pasoAPaso.contains("x = 4"))
        Mockito.verify(solucionarioRepository).save(Mockito.any(Solucionario::class.java))
    }

    /** Comprueba el 404 y que no se consulte solucionarios si falta la pregunta. */
    @Test
    fun `retorna 404 cuando no existe la pregunta`() {
        `when`(preguntaRepository.findById(99L)).thenReturn(Optional.empty())

        val error = assertThrows(ResponseStatusException::class.java) {
            service.generarParaPregunta(99L)
        }

        assertEquals(404, error.statusCode.value())
        Mockito.verifyNoInteractions(solucionarioRepository)
    }

    /** Comprueba el conflicto cuando la pregunta ya tiene solucionario. */
    @Test
    fun `rechaza generar si la pregunta ya tiene solucionario`() {
        `when`(preguntaRepository.findById(42L)).thenReturn(Optional.of(pregunta()))
        `when`(solucionarioRepository.existsByPregunta_Id(42L)).thenReturn(true)

        val error = assertThrows(ResponseStatusException::class.java) {
            service.generarParaPregunta(42L)
        }

        assertEquals(409, error.statusCode.value())
        Mockito.verify(solucionarioRepository, Mockito.never()).save(Mockito.any(Solucionario::class.java))
    }

    /** Comprueba que una respuesta registrada inconsistente produce 422 y no se guarda. */
    @Test
    fun `no inventa solucion para ecuacion no soportada o respuesta inconsistente`() {
        `when`(preguntaRepository.findById(42L)).thenReturn(Optional.of(pregunta(respuesta = "5")))
        `when`(solucionarioRepository.existsByPregunta_Id(42L)).thenReturn(false)

        val error = assertThrows(ResponseStatusException::class.java) {
            service.generarParaPregunta(42L)
        }

        assertEquals(422, error.statusCode.value())
        Mockito.verify(solucionarioRepository, Mockito.never()).save(Mockito.any(Solucionario::class.java))
    }

    /** Comprueba que un enunciado incompatible produce error y no persiste un solucionario. */
    @Test
    fun `no persiste solucionario si el enunciado no tiene ecuacion compatible`() {
        val sinEcuacion = Pregunta(
            id = 42L,
            enunciado = "Calcula el area del triangulo",
            tema = "Geometria",
            dificultad = Dificultad.FACIL,
            opciones = listOf("2", "4"),
            respuestaCorrecta = "4",
            docenteId = 3L
        )
        `when`(preguntaRepository.findById(42L)).thenReturn(Optional.of(sinEcuacion))
        `when`(solucionarioRepository.existsByPregunta_Id(42L)).thenReturn(false)

        val error = assertThrows(ResponseStatusException::class.java) {
            service.generarParaPregunta(42L)
        }

        assertEquals(422, error.statusCode.value())
        Mockito.verify(solucionarioRepository, Mockito.never()).save(Mockito.any(Solucionario::class.java))
    }

    private fun pregunta(respuesta: String = "4") = Pregunta(
        id = 42L,
        enunciado = "¿Cuál es el valor de X si 2x + 6 = 14?",
        tema = "Algebra",
        dificultad = Dificultad.FACIL,
        opciones = listOf("2", "4", "6", "8"),
        respuestaCorrecta = respuesta,
        docenteId = 3L
    )
}
