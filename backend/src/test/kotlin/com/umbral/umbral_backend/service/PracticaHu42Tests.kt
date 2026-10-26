package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.controller.PracticaController
import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.PreguntaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException

class PracticaHu42Tests {
    private val repository = mock(PreguntaRepository::class.java)
    private val service = BancoPreguntasService(repository)

    @Test
    fun `consulta exclusivamente aprobadas por tema y dificultad`() {
        `when`(repository.findByEstadoAndTemaAndDificultad(EstadoPregunta.APROBADA, "Algebra", Dificultad.FACIL))
            .thenReturn(listOf(pregunta()))
        val resultado = service.obtenerPreguntasPractica(" Algebra ", Dificultad.FACIL)
        assertEquals(1, resultado.size)
        assertEquals(listOf("2", "4"), resultado.single().opciones)
        verify(repository).findByEstadoAndTemaAndDificultad(EstadoPregunta.APROBADA, "Algebra", Dificultad.FACIL)
        verifyNoMoreInteractions(repository)
    }

    @Test
    fun `rechaza tema vacio antes de consultar`() {
        val error = assertThrows(ResponseStatusException::class.java) {
            service.obtenerPreguntasPractica("  ", Dificultad.FACIL)
        }
        assertEquals(HttpStatus.BAD_REQUEST, error.statusCode)
        verifyNoInteractions(repository)
    }

    @Test
    fun `devuelve lista vacia cuando no hay coincidencias`() {
        `when`(repository.findByEstadoAndTemaAndDificultad(EstadoPregunta.APROBADA, "Algebra", Dificultad.DIFICIL))
            .thenReturn(emptyList())
        assertTrue(service.obtenerPreguntasPractica("Algebra", Dificultad.DIFICIL).isEmpty())
    }

    @Test
    fun `endpoint integra controlador servicio y serializacion sin filtrar solucion`() {
        `when`(repository.findByEstadoAndTemaAndDificultad(EstadoPregunta.APROBADA, "Algebra", Dificultad.FACIL))
            .thenReturn(listOf(pregunta()))
        val mvc = MockMvcBuilders.standaloneSetup(PracticaController(service)).build()
        mvc.perform(get("/api/practicas/preguntas").param("tema", "Algebra").param("dificultad", "FACIL"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(7))
            .andExpect(jsonPath("$[0].respuestaCorrecta").doesNotExist())
            .andExpect(jsonPath("$[0].docenteId").doesNotExist())
            .andExpect(jsonPath("$[0].estado").doesNotExist())
        mvc.perform(get("/api/practicas/preguntas").param("dificultad", "FACIL"))
            .andExpect(status().isBadRequest)
        mvc.perform(get("/api/practicas/preguntas").param("tema", "Algebra").param("dificultad", "INVALIDA"))
            .andExpect(status().isBadRequest)
        mvc.perform(get("/api/practicas/preguntas").param("tema", " ").param("dificultad", "FACIL"))
            .andExpect(status().isBadRequest)
    }

    private fun pregunta() = Pregunta(
        id = 7, enunciado = "2x + 6 = 14", tema = "Algebra", dificultad = Dificultad.FACIL,
        opciones = listOf("2", "4"), respuestaCorrecta = "4", estado = EstadoPregunta.APROBADA, docenteId = 3
    )
}
