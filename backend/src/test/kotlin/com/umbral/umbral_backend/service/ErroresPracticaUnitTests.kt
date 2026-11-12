package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.*
import com.umbral.umbral_backend.security.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

/** Comprueba mensajes seguros y permisos sin cambiar los códigos de práctica y solucionarios. */
class ErroresPracticaUnitTests {
    private val preguntas=mock(PreguntaRepository::class.java)
    private val respuestas=mock(RespuestaPracticaRepository::class.java)
    private val soluciones=mock(SolucionarioService::class.java)
    private val identidad=mock(IdentidadAcademica::class.java)
    private val alumno=Actor(2,"ALUMNO","alumno@example.invalid")
    private val practica=RespuestaPracticaService(preguntas,respuestas,identidad)
    private val consulta=ConsultaSolucionarioService(preguntas,respuestas,soluciones,identidad)

    private fun pregunta(estado: EstadoPregunta)=Pregunta(id=7,enunciado="2x + 6 = 14",tema="Algebra",
        dificultad=Dificultad.FACIL,opciones=listOf("2","4"),respuestaCorrecta="4",estado=estado,docenteId=1)

    @Test fun `pregunta inexistente conserva 404 con mensaje seguro en ambos servicios`() {
        `when`(preguntas.findById(7)).thenReturn(Optional.empty())
        val errores=listOf(
            assertThrows(ResponseStatusException::class.java) { practica.registrar(alumno,7,"4") },
            assertThrows(ResponseStatusException::class.java) { consulta.consultar(alumno,7) })
        errores.forEach {
            assertEquals(404,it.statusCode.value())
            assertEquals("La pregunta no está disponible",it.reason)
        }
        verifyNoInteractions(respuestas,soluciones,identidad)
    }

    @Test fun `preguntas pendientes y rechazadas usan el mismo error que una inexistente`() {
        for(estado in listOf(EstadoPregunta.PENDIENTE,EstadoPregunta.RECHAZADA)) {
            `when`(preguntas.findById(7)).thenReturn(Optional.of(pregunta(estado)))
            val errores=listOf(
                assertThrows(ResponseStatusException::class.java) { practica.registrar(alumno,7,"4") },
                assertThrows(ResponseStatusException::class.java) { consulta.consultar(alumno,7) })
            errores.forEach {
                assertEquals(404,it.statusCode.value())
                assertEquals("La pregunta no está disponible",it.reason)
            }
        }
        verifyNoInteractions(respuestas,soluciones,identidad)
    }

    @Test fun `consulta anticipada mantiene 403 sin consultar la solucion`() {
        `when`(preguntas.findById(7)).thenReturn(Optional.of(pregunta(EstadoPregunta.APROBADA)))
        val error=assertThrows(ResponseStatusException::class.java) { consulta.consultar(alumno,7) }
        assertEquals(403,error.statusCode.value())
        assertEquals("Debe responder primero",error.reason)
        verifyNoInteractions(soluciones)
    }

    @Test fun `consulta autorizada sigue reutilizando el solucionario aprobado`() {
        val p=pregunta(EstadoPregunta.APROBADA)
        `when`(preguntas.findById(7)).thenReturn(Optional.of(p))
        `when`(respuestas.existsByAlumnoIdAndPreguntaId(2,7)).thenReturn(true)
        `when`(soluciones.aprobadoParaPregunta(7)).thenReturn(Solucionario(pregunta=p,pasoAPaso="Restar 6 y dividir entre 2",estado=EstadoSolucionario.APROBADO))
        val resultado=consulta.consultar(alumno,7)
        assertEquals(7,resultado.preguntaId)
        assertEquals("4",resultado.respuestaCorrecta)
        assertEquals("Restar 6 y dividir entre 2",resultado.pasoAPaso)
        verify(identidad).exigirPregunta(2,1)
        verify(soluciones).aprobadoParaPregunta(7)
    }
}
