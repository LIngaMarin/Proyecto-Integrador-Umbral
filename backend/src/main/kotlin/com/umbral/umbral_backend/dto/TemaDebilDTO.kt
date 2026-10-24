package com.umbral.umbral_backend.dto

/** Resultado agregado del cálculo de errores para un tema. */
data class TemaDebilDTO(
    /** Nombre del tema tal como aparece en los resultados procesados. */
    val tema: String,
    /** Número de apariciones no vacías contabilizadas para el tema. */
    val cantidadErrores: Int
)
