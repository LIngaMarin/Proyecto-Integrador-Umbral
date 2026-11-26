package com.umbral.app

import com.umbral.app.domain.Clase
import com.umbral.app.domain.MomentoSemana
import com.umbral.app.domain.momentoActual
import com.umbral.app.domain.proximaClase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class HorarioTest {

    private fun clase(id: Int, dia: Int, inicio: String, fin: String) =
        Clase(id, "Curso $id", "A", "A-201", "Sede", dia, inicio, fin, "Docente")

    private val semana = listOf(
        clase(1, dia = 1, inicio = "08:00", fin = "09:30"), // lunes
        clase(2, dia = 3, inicio = "10:00", fin = "11:30"), // miercoles
        clase(3, dia = 5, inicio = "08:00", fin = "09:30"), // viernes
    )

    @Test
    fun elMartesLaProximaEsLaDelMiercoles() {
        assertEquals(2, proximaClase(semana, MomentoSemana(2, "12:00"))?.id)
    }

    @Test
    fun unaClaseEnCursoCuentaComoLaProxima() {
        assertEquals(1, proximaClase(semana, MomentoSemana(1, "09:00"))?.id)
    }

    @Test
    fun siYaPasaronTodasVuelveAlLunes() {
        assertEquals(1, proximaClase(semana, MomentoSemana(6, "10:00"))?.id)
    }

    @Test
    fun sinClasesNoHayProxima() {
        assertNull(proximaClase(emptyList(), MomentoSemana(1, "08:00")))
    }

    @Test
    fun elDomingoEsElDia7YElLunesEl1() {
        val domingo = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 29, 10, 5) }
        val lunes = Calendar.getInstance().apply { set(2026, Calendar.NOVEMBER, 30, 8, 0) }
        assertEquals(MomentoSemana(7, "10:05"), momentoActual(domingo))
        assertEquals(MomentoSemana(1, "08:00"), momentoActual(lunes))
    }
}
