package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.PreguntaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional

/** Ejecuta Flyway y la consulta derivada real contra H2 en modo PostgreSQL. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PracticaPersistenciaHu42Tests @Autowired constructor(
    private val repository: PreguntaRepository,
    private val service: BancoPreguntasService
) {
    @Test
    fun `la consulta persistida excluye pendientes rechazadas otros temas y dificultades`() {
        fun guardar(estado: EstadoPregunta, tema: String = "Algebra", dificultad: Dificultad = Dificultad.FACIL) =
            repository.saveAndFlush(Pregunta(
                enunciado = "2x + 6 = 14", tema = tema, dificultad = dificultad,
                opciones = listOf("2", "4"), respuestaCorrecta = "4", estado = estado, docenteId = 3
            ))
        val aprobada = guardar(EstadoPregunta.APROBADA)
        guardar(EstadoPregunta.PENDIENTE)
        guardar(EstadoPregunta.RECHAZADA)
        guardar(EstadoPregunta.APROBADA, tema = "Geometria")
        guardar(EstadoPregunta.APROBADA, dificultad = Dificultad.DIFICIL)

        assertEquals(listOf(aprobada.id), service.obtenerPreguntasPractica("Algebra", Dificultad.FACIL).map { it.id })
    }
}
