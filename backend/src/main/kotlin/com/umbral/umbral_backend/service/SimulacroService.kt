package com.umbral.umbral_backend.service

import com.umbral.umbral_backend.dto.TemaDebilDTO
import com.umbral.umbral_backend.repository.ResultadoSimulacroRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper

@Service
/** Calcula un resumen de los temas incorrectos registrados por sección. */
class SimulacroService(
    private val resultadoRepository: ResultadoSimulacroRepository,
    private val objectMapper: ObjectMapper
) {
    /** Cuenta temas no vacíos y ordena por cantidad descendente y luego por nombre. */
    fun calcularTemasDebiles(seccionId: Long): List<TemaDebilDTO> {
        val resultados = resultadoRepository.findBySeccionId(seccionId)
        val erroresPorTema = mutableMapOf<String, Int>()

        resultados.forEach { resultado ->
            leerTemasIncorrectos(resultado.temasIncorrectosJson).forEach { tema ->
                if (tema.isNotBlank()) {
                    erroresPorTema[tema] = erroresPorTema.getOrDefault(tema, 0) + 1
                }
            }
        }

        return erroresPorTema.map { (tema, cantidad) -> TemaDebilDTO(tema, cantidad) }
            .sortedWith(compareByDescending<TemaDebilDTO> { it.cantidadErrores }.thenBy { it.tema })
    }

    /** Interpreta el campo como un arreglo JSON de textos y recorta cada elemento. */
    private fun leerTemasIncorrectos(json: String): List<String> {
        val array = try {
            objectMapper.readTree(json)
        } catch (_: Exception) {
            throw jsonInvalido()
        }

        if (!array.isArray) throw jsonInvalido()
        return buildList {
            array.forEach { nodo ->
                if (!nodo.isString) throw jsonInvalido()
                add(nodo.asString().trim())
            }
        }
    }

    /** Crea el error usado cuando el JSON no se puede leer o no tiene la estructura esperada. */
    private fun jsonInvalido() = ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "Un resultado contiene temasIncorrectosJson inválido; se esperaba un arreglo JSON de textos."
    )
}
