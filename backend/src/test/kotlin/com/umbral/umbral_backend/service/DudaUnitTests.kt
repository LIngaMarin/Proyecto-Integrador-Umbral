package com.umbral.umbral_backend.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Comprueba la normalización de los temas utilizados para agrupar dudas. */
class DudaUnitTests {
    @Test fun `normalizacion agrupa tema equivalente sin alterar texto de la duda`() {
        assertEquals("algebra lineal",DudaService.normalizarTema("  ÁLGEBRA   Lineal  "))
        assertEquals(DudaService.normalizarTema("álgebra"),DudaService.normalizarTema("Algebra"))
        assertNotEquals(DudaService.normalizarTema("Algebra"),DudaService.normalizarTema("Geometria"))
    }
}
