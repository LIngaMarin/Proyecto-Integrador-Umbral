package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.TemaDebilDTO
import com.umbral.umbral_backend.model.ResultadoSimulacro
import com.umbral.umbral_backend.repository.ResultadoSimulacroRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper

class SimulacroServiceHu25Tests {
    private val repository = Mockito.mock(ResultadoSimulacroRepository::class.java)
    private val service = SimulacroService(repository, ObjectMapper())

    /** Verifica el conteo por tema y el orden descendente, con nombre como desempate. */
    @Test
    fun `cuenta errores por tema y ordena de mayor a menor`() {
        `when`(repository.findBySeccionId(8L)).thenReturn(
            listOf(
                resultado(1L, "[\"Algebra\", \"Geometria\"]"),
                resultado(2L, "[\"Algebra\", \"Estadistica\"]"),
                resultado(3L, "[\"Algebra\"]")
            )
        )

        assertEquals(
            listOf(
                TemaDebilDTO("Algebra", 3),
                TemaDebilDTO("Estadistica", 1),
                TemaDebilDTO("Geometria", 1)
            ),
            service.calcularTemasDebiles(8L)
        )
        Mockito.verify(repository).findBySeccionId(8L)
        Mockito.verifyNoMoreInteractions(repository)
    }

    /** Comprueba que una sección sin resultados produce una lista vacía. */
    @Test
    fun `devuelve lista vacia si la seccion no tiene resultados`() {
        `when`(repository.findBySeccionId(9L)).thenReturn(emptyList())

        assertEquals(emptyList<TemaDebilDTO>(), service.calcularTemasDebiles(9L))
    }

    /** Comprueba que un JSON incompleto produce el error 422. */
    @Test
    fun `rechaza JSON malformado con error 422`() {
        `when`(repository.findBySeccionId(8L)).thenReturn(listOf(resultado(1L, "[\"Algebra\"")))

        val error = assertThrows(ResponseStatusException::class.java) {
            service.calcularTemasDebiles(8L)
        }

        assertEquals(422, error.statusCode.value())
    }

    /** Comprueba que un JSON que no sea arreglo de textos también produce 422. */
    @Test
    fun `rechaza JSON que no contiene un arreglo de textos`() {
        `when`(repository.findBySeccionId(8L)).thenReturn(listOf(resultado(1L, "{\"tema\":\"Algebra\"}")))

        val error = assertThrows(ResponseStatusException::class.java) {
            service.calcularTemasDebiles(8L)
        }

        assertEquals(422, error.statusCode.value())
    }

    private fun resultado(id: Long, temasIncorrectosJson: String) = ResultadoSimulacro(
        id = id,
        simulacroId = 101L,
        alumnoId = id,
        seccionId = 8L,
        puntaje = 0.0,
        temasIncorrectosJson = temasIncorrectosJson
    )
}
