package com.umbral.app

import com.umbral.app.data.demo.CLAVE_DEMO
import com.umbral.app.data.demo.DemoRepository
import com.umbral.app.domain.ErrorUmbral
import com.umbral.app.domain.Rol
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoRepositoryTest {

    private val repo = DemoRepository(retardoMs = 0)

    @Test
    fun cadaPreguntaTieneCuatroOpcionesYCorrectaValida() = runTest {
        val preguntas = repo.temas().getOrThrow().flatMap { repo.preguntasPorTema(it).getOrThrow() }
        assertTrue(preguntas.size >= 18)
        preguntas.forEach { p ->
            assertEquals("Pregunta ${p.id}: debe tener 4 opciones", 4, p.opciones.size)
            assertTrue("Pregunta ${p.id}: correcta fuera de rango", p.correcta in p.opciones.indices)
            assertTrue("Pregunta ${p.id}: solucionario vacío", p.solucionario.isNotEmpty())
        }
    }

    @Test
    fun idsDePreguntasSonUnicos() = runTest {
        val preguntas = repo.temas().getOrThrow().flatMap { repo.preguntasPorTema(it).getOrThrow() }
        assertEquals(preguntas.size, preguntas.map { it.id }.toSet().size)
    }

    @Test
    fun preguntasSemanaTieneSeisPreguntas() = runTest {
        assertEquals(6, repo.preguntasSemana().getOrThrow().size)
    }

    @Test
    fun preguntasPorTemaDevuelveSoloEseTema() = runTest {
        repo.temas().getOrThrow().forEach { tema ->
            val preguntas = repo.preguntasPorTema(tema).getOrThrow()
            assertTrue("Tema $tema: menos de 3 preguntas", preguntas.size >= 3)
            assertTrue("Tema $tema: pregunta de otro tema", preguntas.all { it.tema == tema })
        }
    }

    @Test
    fun progresoTieneCompletadasMenoresOIgualesQueTotales() = runTest {
        repo.progreso().getOrThrow().forEach { curso ->
            assertTrue("${curso.curso}: completadas > totales", curso.completadas <= curso.totales)
        }
    }

    @Test
    fun responderDudaLaMarcaComoResueltaConSuRespuesta() = runTest {
        val antes = repo.dudas().getOrThrow().first { it.id == 1 }
        assertFalse(antes.resuelta)
        assertNull(antes.respuesta)

        repo.responderDuda(1, "Respuesta de prueba")

        val despues = repo.dudas().getOrThrow().first { it.id == 1 }
        assertTrue(despues.resuelta)
        assertEquals("Respuesta de prueba", despues.respuesta)
        assertEquals(antes.texto, despues.texto)
    }

    @Test
    fun marcarAvisoLeidoCambiaElEstado() = runTest {
        assertFalse(repo.avisos().getOrThrow().first { it.id == 1 }.leida)
        repo.marcarAvisoLeido(1)
        assertTrue(repo.avisos().getOrThrow().first { it.id == 1 }.leida)
    }

    @Test
    fun loginCorrectoDevuelveElRol() = runTest {
        val sesion = repo.login("marcos@umbral.pe", CLAVE_DEMO).getOrThrow()
        assertEquals(Rol.DOCENTE, sesion.usuario.rol)
        assertTrue(sesion.token.isNotBlank())
    }

    @Test
    fun loginConClaveIncorrectaFallaConCredencialesInvalidas() = runTest {
        val resultado = repo.login("camila@umbral.pe", "otra")
        assertTrue(resultado.exceptionOrNull() is ErrorUmbral.CredencialesInvalidas)
    }

    @Test
    fun marcarFavoritaSeGuarda() = runTest {
        repo.marcarFavorita(5, true)
        assertTrue(5 in repo.marcasPractica().getOrThrow().favoritas)
        repo.marcarFavorita(5, false)
        assertFalse(5 in repo.marcasPractica().getOrThrow().favoritas)
    }
}
