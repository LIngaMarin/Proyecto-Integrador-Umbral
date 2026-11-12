package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.ActualizarPreguntaDTO
import com.umbral.umbral_backend.dto.AprobarRechazarDTO
import com.umbral.umbral_backend.dto.PreguntaPracticaDTO
import com.umbral.umbral_backend.model.*
import com.umbral.umbral_backend.repository.PreguntaRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.multipart.MultipartFile

@Service
/** Coordina las consultas y operaciones de revisión del banco de preguntas. */
class BancoPreguntasService(
    private val preguntaRepository: PreguntaRepository
) {
    /** HU 42: consulta solo preguntas publicadas sin revelar su solución. */
    @Transactional(readOnly = true)
    fun obtenerPreguntasPractica(tema: String, dificultad: Dificultad): List<PreguntaPracticaDTO> {
        if (tema.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "El tema es obligatorio")
        }
        return preguntaRepository.findByEstadoAndTemaAndDificultad(
            EstadoPregunta.APROBADA, tema.trim(), dificultad
        ).map {
            PreguntaPracticaDTO(
                it.id, it.enunciado, it.tema, it.dificultad, it.opciones.toList()
            )
        }
    }

    // HU 22: Consultar las preguntas de un docente con filtros opcionales.
    /** Mantiene el filtro docente y combina los filtros opcionales presentes. */
    fun obtenerPreguntas(docenteId: Long, tema: String?, dificultad: Dificultad?): List<Pregunta> =
        when {
            tema != null && dificultad != null ->
                preguntaRepository.findByDocenteIdAndTemaAndDificultad(docenteId, tema, dificultad)
            tema != null -> preguntaRepository.findByDocenteIdAndTema(docenteId, tema)
            dificultad != null -> preguntaRepository.findByDocenteIdAndDificultad(docenteId, dificultad)
            else -> preguntaRepository.findByDocenteId(docenteId)
        }

    // HU 18: Extraer preguntas desde boletín (PDF/Imagen)
    /** Importa únicamente el contenido validado del PDF; toda la operación se confirma o revierte junta. */
    @Transactional
    fun extraerPreguntasDeBoletin(file: MultipartFile, docenteId: Long): List<Pregunta> {
        return preguntaRepository.saveAll(ImportadorBoletin.leer(file,docenteId))
    }

    // Genera variantes mediante reglas matemáticas verificables, sin atribuirlas a IA.
    /** Variante matemática de una ecuación lineal verificada; no se presenta como una integración IA. */
    @Transactional
    fun generarVariante(preguntaId: Long, docenteId: Long): Pregunta {
        val original = preguntaRepository.findById(preguntaId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND,"Pregunta no encontrada")
        }
        if(original.docenteId!=docenteId)throw ResponseStatusException(HttpStatus.FORBIDDEN)
        val match=Regex("([+-]?\\d+)\\s*[xX]\\s*([+-])\\s*(\\d+)\\s*=\\s*([+-]?\\d+)").find(original.enunciado)
            ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"Solo se admiten variantes de ecuaciones lineales ax ± b = c")
        val a=match.groupValues[1].toBigDecimal()
        val b=match.groupValues[3].toBigDecimal().let{if(match.groupValues[2]=="-")it.negate()else it}
        val c=match.groupValues[4].toBigDecimal()
        val x=original.respuestaCorrecta.toBigDecimalOrNull()
            ?: throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"La respuesta debe ser numérica")
        if(a.signum()==0 || (a*x+b).compareTo(c)!=0 || x.stripTrailingZeros().scale()>0)
            throw ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"La ecuación debe tener una respuesta entera verificada")
        val nuevoA=if(a+java.math.BigDecimal.ONE==java.math.BigDecimal.ZERO)a+java.math.BigDecimal.TWO else a+java.math.BigDecimal.ONE
        val nuevoX=x+java.math.BigDecimal.ONE
        fun texto(n:java.math.BigDecimal)=n.stripTrailingZeros().toPlainString()
        val variante = Pregunta(
            enunciado = "Resuelve ${texto(nuevoA)}x ${if(b.signum()<0)"-" else "+"} ${texto(b.abs())} = ${texto(nuevoA*nuevoX+b)}",
            tema = original.tema,
            dificultad = original.dificultad,
            opciones = listOf(-1,0,1,2).map{texto(nuevoX+it.toBigDecimal())},
            respuestaCorrecta = texto(nuevoX),
            estado = EstadoPregunta.PENDIENTE,
            docenteId = docenteId
        )
        return preguntaRepository.save(variante)
    }

    // HU 20: Revisar, editar y aprobar pregunta
    /** Actualiza los campos del DTO, aprueba la pregunta y limpia el motivo anterior. */
    fun editarYAprobar(id: Long, dto: ActualizarPreguntaDTO): Pregunta {
        val pregunta = preguntaRepository.findById(id).orElseThrow {
            RuntimeException("Pregunta no encontrada con el id: $id")
        }

        pregunta.enunciado = dto.enunciado
        pregunta.tema = dto.tema
        pregunta.dificultad = dto.dificultad
        pregunta.opciones = dto.opciones
        pregunta.respuestaCorrecta = dto.respuestaCorrecta
        pregunta.estado = EstadoPregunta.APROBADA
        pregunta.motivoRechazo = null

        return preguntaRepository.save(pregunta)
    }

    // HU 21: Rechazar preguntas con motivo
    /** Exige un motivo, lo recorta, cambia el estado y guarda la pregunta rechazada. */
    fun rechazarPregunta(id: Long, dto: AprobarRechazarDTO): Pregunta {
        // Evita consultar el repositorio si el motivo solo contiene espacios.
        require(dto.motivoRechazo.isNotBlank()) { "El motivo de rechazo es obligatorio" }

        val pregunta = preguntaRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Pregunta no encontrada con el id: $id")
        }

        pregunta.estado = EstadoPregunta.RECHAZADA
        pregunta.motivoRechazo = dto.motivoRechazo.trim()

        return preguntaRepository.save(pregunta)
    }
}
