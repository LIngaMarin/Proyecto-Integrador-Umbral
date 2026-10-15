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
class SolucionarioService(
    private val preguntaRepository: PreguntaRepository,
    private val solucionarioRepository: SolucionarioRepository
) {
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

    private fun resolverEcuacionLineal(pregunta: Pregunta): String {
        val match = ECUACION_LINEAL.find(pregunta.enunciado)
            ?: throw preguntaNoResuelta()

        val coeficiente = match.groupValues[1].toBigDecimalOrNull() ?: throw preguntaNoResuelta()
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

    private fun formatear(valor: BigDecimal): String = valor.stripTrailingZeros().toPlainString()

    private fun preguntaNoResuelta() = ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "No se puede generar una solución verificada para esta pregunta; se admite una ecuación lineal del tipo ax ± b = c y una respuesta numérica correcta."
    )

    private companion object {
        val ECUACION_LINEAL = Regex("""([+-]?\d+)\s*[xX]\s*([+-])\s*(\d+)\s*=\s*([+-]?\d+)""")
    }
}
