package com.umbral.app

import com.umbral.app.data.demo.DemoRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoRepositoryTest {

    private val repo = DemoRepository()

    @Test
    fun cadaPreguntaTieneCuatroOpcionesYCorrectaValida() {
        runBlocking {
            val preguntas = repo.temas().flatMap { repo.preguntasPorTema(it) }
            assertTrue(preguntas.size >= 18)
            preguntas.forEach { p ->
                assertEquals("Pregunta ${p.id}: debe tener 4 opciones", 4, p.opciones.size)
                assertTrue("Pregunta ${p.id}: correcta fuera de rango", p.correcta in p.opciones.indices)
                assertTrue("Pregunta ${p.id}: solucionario vacío", p.solucionario.isNotEmpty())
            }
        }
    }

    @Test
    fun idsDePreguntasSonUnicos() {
        runBlocking {
            val preguntas = repo.temas().flatMap { repo.preguntasPorTema(it) }
            assertEquals(preguntas.size, preguntas.map { it.id }.toSet().size)
        }
    }

    @Test
    fun preguntasSemanaTieneSeisPreguntas() {
        runBlocking {
            assertEquals(6, repo.preguntasSemana().size)
        }
    }

    @Test
    fun preguntasPorTemaDevuelveSoloEseTema() {
        runBlocking {
            repo.temas().forEach { tema ->
                val preguntas = repo.preguntasPorTema(tema)
                assertTrue("Tema $tema: menos de 3 preguntas", preguntas.size >= 3)
                assertTrue("Tema $tema: pregunta de otro tema", preguntas.all { it.tema == tema })
            }
        }
    }

    @Test
    fun progresoTieneCompletadasMenoresOIgualesQueTotales() {
        runBlocking {
            repo.progreso().forEach { curso ->
                assertTrue("${curso.curso}: completadas > totales", curso.completadas <= curso.totales)
            }
        }
    }

    @Test
    fun responderDudaLaMarcaComoResueltaConSuRespuesta() {
        runBlocking {
            val antes = repo.dudas().first { it.id == 1 }
            assertFalse(antes.resuelta)
            assertNull(antes.respuesta)

            repo.responderDuda(1, "Respuesta de prueba")

            val despues = repo.dudas().first { it.id == 1 }
            assertTrue(despues.resuelta)
            assertEquals("Respuesta de prueba", despues.respuesta)
            assertEquals(antes.texto, despues.texto)
        }
    }

    @Test
    fun marcarAvisoLeidoCambiaElEstado() {
        runBlocking {
            assertFalse(repo.avisos().first { it.id == 1 }.leida)

            repo.marcarAvisoLeido(1)

            assertTrue(repo.avisos().first { it.id == 1 }.leida)
        }
    }
}
