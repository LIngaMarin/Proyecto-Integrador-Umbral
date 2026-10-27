package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.ResumenSemanal
import com.umbral.umbral_backend.repository.ResumenSemanalRepository
import com.umbral.umbral_backend.security.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.scheduling.support.CronExpression
import org.springframework.web.server.ResponseStatusException
import java.time.*

class ResumenSemanalUnitTests {
    @Test fun `semana exige lunes completo y cron corresponde al lunes ocho de Lima`() {
        val lunes=LocalDate.of(2026,10,19)
        assertDoesNotThrow { EstadisticasSemanaService.validarSemana(lunes,lunes.plusWeeks(1)) }
        assertThrows(ResponseStatusException::class.java) { EstadisticasSemanaService.validarSemana(lunes.plusDays(1),lunes.plusWeeks(2)) }
        assertThrows(ResponseStatusException::class.java) { EstadisticasSemanaService.validarSemana(lunes,lunes.plusDays(6)) }
        val desde=ZonedDateTime.of(2026,10,27,0,0,0,0,ZoneId.of("America/Lima"))
        val siguiente=CronExpression.parse("0 0 8 * * MON").next(desde)!!
        assertEquals(DayOfWeek.MONDAY,siguiente.dayOfWeek)
        assertEquals(8,siguiente.hour)
    }

    @Test fun `fallo de correo se conserva para reintento y envio exitoso no se repite`() {
        val repo=mock(ResumenSemanalRepository::class.java)
        val identidad=mock(IdentidadAcademica::class.java)
        val stats=mock(EstadisticasSemanaService::class.java)
        val clock=Clock.fixed(Instant.parse("2026-10-26T13:00:00Z"),ZoneId.of("America/Lima"))
        val r=ResumenSemanal(id=1,docenteId=1,semanaInicio=LocalDate.of(2026,10,19),
            destinatario="docente@example.invalid",asunto="Resumen",cuerpo="Datos")
        `when`(repo.bloquear(1)).thenReturn(r)
        `when`(identidad.docentes()).thenReturn(listOf(Actor(1,"DOCENTE",r.destinatario)))
        var llamadas=0
        val correo=object : CorreoResumen {
            override val simulado=false
            override fun enviar(destinatario: String,asunto: String,cuerpo: String) {
                llamadas++
                if(llamadas==1) throw IllegalStateException("Error SMTP de prueba")
            }
        }
        val service=ResumenSemanalService(repo,stats,identidad,correo,clock)
        service.enviar(1)
        assertEquals("ERROR",r.estado)
        assertEquals(1,r.intentos)
        assertEquals("IllegalStateException",r.ultimoError)
        service.enviar(1)
        assertEquals("ENVIADO",r.estado)
        assertEquals(2,r.intentos)
        assertNotNull(r.enviado)
        assertNull(r.ultimoError)
        service.enviar(1)
        assertEquals(2,llamadas)
    }
}
