package com.umbral.umbral_backend.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDate

class CalendarioUnitTests {
    @Test fun `rango inclusivo admite un dia y rechaza fechas invertidas`() {
        val fecha=LocalDate.of(2026,11,1)
        assertDoesNotThrow { CalendarioService.validarRango(fecha,fecha) }
        assertEquals(400,assertThrows(ResponseStatusException::class.java) {
            CalendarioService.validarRango(fecha,fecha.minusDays(1))
        }.statusCode.value())
    }
}
