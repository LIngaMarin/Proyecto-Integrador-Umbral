package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.model.EstadoSolucionario
import com.umbral.umbral_backend.model.Pregunta
import com.umbral.umbral_backend.model.Solucionario
import com.umbral.umbral_backend.repository.PreguntaRepository
import com.umbral.umbral_backend.repository.SolucionarioRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.math.BigDecimal

@Service
/** Genera solucionarios mediante reglas programadas y administra su estado de revisión. */
class SolucionarioService(
    private val preguntaRepository: PreguntaRepository,
    private val solucionarioRepository: SolucionarioRepository
) {
    /** Devuelve el solucionario aprobado de la pregunta; si falta o sigue en borrador, responde con 404. */
    fun aprobadoParaPregunta(preguntaId: Long): Solucionario =
        solucionarioRepository.findByPregunta_Id(preguntaId)?.takeIf { it.estado == EstadoSolucionario.APROBADO }
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "No hay solución aprobada")

    /** Busca la pregunta, evita duplicados y guarda como borrador los pasos que puede verificar. */
    fun generarParaPregunta(preguntaId: Long): Solucionario {
        val pregunta = preguntaRepository.findById(preguntaId).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Pregunta no encontrada con el id: $preguntaId")
        }

        if (solucionarioRepository.existsByPregunta_Id(preguntaId)) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "La pregunta ya tiene un solucionario"
            )
        }

        val pasos = resolverEcuacionLineal(pregunta)
        val solucionario = Solucionario(
            pregunta = pregunta,
            pasoAPaso = pasos,
            estado = EstadoSolucionario.BORRADOR
        )
        return solucionarioRepository.save(solucionario)
    }

    /** Cambia un solucionario existente de BORRADOR a APROBADO; otros estados producen conflicto. */
    fun aprobar(id: Long): Solucionario {
        val solucionario = solucionarioRepository.findById(id).orElseThrow {
            ResponseStatusException(HttpStatus.NOT_FOUND, "Solucionario no encontrado con el id: $id")
        }

        if (solucionario.estado != EstadoSolucionario.BORRADOR) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "Solo se pueden aprobar solucionarios en estado BORRADOR"
            )
        }

        solucionario.estado = EstadoSolucionario.APROBADO
        return solucionarioRepository.save(solucionario)
    }

    /** Resuelve el patrón ax ± b = c con coeficientes enteros reconocidos por la expresión regular. */
    private fun resolverEcuacionLineal(pregunta: Pregunta): String {
        val match = ECUACION_LINEAL.find(pregunta.enunciado)
            ?: throw preguntaNoResuelta()

        val coeficiente = match.groupValues[1].toBigDecimalOrNull() ?: throw preguntaNoResuelta()
        // No se puede despejar x si su coeficiente es cero.
        if (coeficiente.compareTo(BigDecimal.ZERO) == 0) throw preguntaNoResuelta()

        val signo = match.groupValues[2]
        val magnitud = match.groupValues[3].toBigDecimalOrNull() ?: throw preguntaNoResuelta()
        val terminoIndependiente = if (signo == "+") magnitud else magnitud.negate()
        val resultadoEcuacion = match.groupValues[4].toBigDecimalOrNull() ?: throw preguntaNoResuelta()
        val valorDespuesDeDespejar = resultadoEcuacion.subtract(terminoIndependiente)
        val valorVariable = try {
            valorDespuesDeDespejar.divide(coeficiente)
        } catch (_: ArithmeticException) {
            throw preguntaNoResuelta()
        }
        // Comprueba la igualdad calculada y que coincida con la respuesta numérica registrada.
        if (coeficiente.multiply(valorVariable).add(terminoIndependiente)
                .compareTo(resultadoEcuacion) != 0
        ) throw preguntaNoResuelta()

        val respuestaEsperada = pregunta.respuestaCorrecta.toBigDecimalOrNull()
            ?: throw preguntaNoResuelta()
        if (valorVariable.compareTo(respuestaEsperada) != 0) throw preguntaNoResuelta()

        val ecuacion = match.value
        val primerPaso = if (signo == "+") {
            "Restamos $magnitud a ambos lados: ${formatear(coeficiente)}x = ${formatear(valorDespuesDeDespejar)}."
        } else {
            "Sumamos $magnitud a ambos lados: ${formatear(coeficiente)}x = ${formatear(valorDespuesDeDespejar)}."
        }

        return listOf(
            "1. Partimos de la ecuación $ecuacion.",
            "2. $primerPaso",
            "3. Dividimos ambos lados entre ${formatear(coeficiente)}: x = ${formatear(valorVariable)}.",
            "4. Comprobamos el resultado: al sustituir x = ${formatear(valorVariable)}, la ecuación se cumple."
        ).joinToString("\n")
    }

    /** Evita notación científica y ceros decimales finales en los pasos generados. */
    private fun formatear(valor: BigDecimal): String = valor.stripTrailingZeros().toPlainString()

    /** Error común para enunciados no reconocidos o soluciones que no coinciden con la respuesta guardada. */
    private fun preguntaNoResuelta() = ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "No se puede generar una solución verificada para esta pregunta; se admite una ecuación lineal del tipo ax ± b = c y una respuesta numérica correcta."
    )

    private companion object {
        val ECUACION_LINEAL = Regex("""([+-]?\d+)\s*[xX]\s*([+-])\s*(\d+)\s*=\s*([+-]?\d+)""")
    }
}
