package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.Dificultad
import com.umbral.umbral_backend.model.Pregunta
import com.umbral.umbral_backend.repository.PreguntaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`

class BancoPreguntasServiceHu22Tests {
    private val repository = Mockito.mock(PreguntaRepository::class.java)
    private val service = BancoPreguntasService(repository)

    @Test
    fun `sin filtros consulta todas las preguntas del docente`() {
        val preguntas = listOf(pregunta())
        `when`(repository.findByDocenteId(3L)).thenReturn(preguntas)

        assertEquals(preguntas, service.obtenerPreguntas(3L, null, null))
        Mockito.verify(repository).findByDocenteId(3L)
        Mockito.verifyNoMoreInteractions(repository)
    }

    @Test
    fun `filtra por tema dentro del docente`() {
        val preguntas = listOf(pregunta())
        `when`(repository.findByDocenteIdAndTema(3L, "Algebra")).thenReturn(preguntas)

        assertEquals(preguntas, service.obtenerPreguntas(3L, "Algebra", null))
        Mockito.verify(repository).findByDocenteIdAndTema(3L, "Algebra")
        Mockito.verifyNoMoreInteractions(repository)
    }

    @Test
    fun `filtra por dificultad dentro del docente`() {
        val preguntas = listOf(pregunta())
        `when`(repository.findByDocenteIdAndDificultad(3L, Dificultad.FACIL)).thenReturn(preguntas)

        assertEquals(preguntas, service.obtenerPreguntas(3L, null, Dificultad.FACIL))
        Mockito.verify(repository).findByDocenteIdAndDificultad(3L, Dificultad.FACIL)
        Mockito.verifyNoMoreInteractions(repository)
    }

    @Test
    fun `combina tema y dificultad manteniendo el docente`() {
        val preguntas = listOf(pregunta())
        `when`(repository.findByDocenteIdAndTemaAndDificultad(3L, "Algebra", Dificultad.FACIL))
            .thenReturn(preguntas)

        assertEquals(preguntas, service.obtenerPreguntas(3L, "Algebra", Dificultad.FACIL))
        Mockito.verify(repository).findByDocenteIdAndTemaAndDificultad(3L, "Algebra", Dificultad.FACIL)
        Mockito.verifyNoMoreInteractions(repository)
    }

    private fun pregunta() = Pregunta(
        enunciado = "Test question",
        tema = "Algebra",
        dificultad = Dificultad.FACIL,
        opciones = listOf("A", "B"),
        respuestaCorrecta = "A",
        docenteId = 3L
    )
}
