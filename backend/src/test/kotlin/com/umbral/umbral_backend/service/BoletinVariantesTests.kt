package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.PreguntaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

/** Usa un PDF real y comprueba que los ejercicios procedan del documento, no de una plantilla fija. */
class BoletinVariantesTests {
    @Test fun `importa contenido real de PDF y rechaza documentos incompletos`() {
        val lineas=listOf("Tema: Aritmetica","Dificultad: INTERMEDIO","Pregunta: Cuanto es 8 + 7?","A) 13","B) 15","C) 17","Correcta: B")
        val pdf=DocumentoPdf().generar(lineas)
        val preguntas=ImportadorBoletin.leer(MockMultipartFile("file","boletin.pdf","application/pdf",pdf),1)
        assertEquals(1,preguntas.size)
        assertEquals("Cuanto es 8 + 7?",preguntas[0].enunciado)
        assertEquals("15",preguntas[0].respuestaCorrecta)
        assertEquals(Dificultad.INTERMEDIO,preguntas[0].dificultad)
        assertEquals(EstadoPregunta.PENDIENTE,preguntas[0].estado)
        assertThrows(ResponseStatusException::class.java){ImportadorBoletin.interpretar("Documento sin preguntas",1)}
        assertThrows(ResponseStatusException::class.java){ImportadorBoletin.leer(MockMultipartFile("file","no.pdf","application/pdf","texto".toByteArray()),1)}
    }
    @Test fun `variante modifica coeficientes y verifica respuesta sin invocar IA`() {
        val repo=Mockito.mock(PreguntaRepository::class.java)
        val base=Pregunta(id=8,enunciado="2x + 6 = 14",tema="Algebra",dificultad=Dificultad.FACIL,respuestaCorrecta="4",docenteId=1)
        `when`(repo.findById(8)).thenReturn(Optional.of(base))
        `when`(repo.save(Mockito.any(Pregunta::class.java))).thenAnswer{it.getArgument<Pregunta>(0)}
        val service=BancoPreguntasService(repo)
        val variante=service.generarVariante(8,1)
        assertEquals("Resuelve 3x + 6 = 21",variante.enunciado)
        assertEquals("5",variante.respuestaCorrecta)
        assertTrue(variante.opciones.contains("5"))
        assertEquals(EstadoPregunta.PENDIENTE,variante.estado)
        assertThrows(ResponseStatusException::class.java){service.generarVariante(8,5)}
        base.respuestaCorrecta="incorrecta"
        assertThrows(ResponseStatusException::class.java){service.generarVariante(8,1)}
    }
}
