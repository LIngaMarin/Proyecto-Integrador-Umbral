package com.umbral.umbral_backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/** Registro de puntaje y temas incorrectos de un alumno en una sección y simulacro. */
@Entity
@Table(name = "resultados_simulacro")
class ResultadoSimulacro(
    /** Identificador generado para este registro de resultado. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Referencias numéricas al simulacro, alumno y sección asociados. */
    val simulacroId: Long,
    val alumnoId: Long,
    val seccionId: Long,
    var puntaje: Double,

    /** JSON guardado como texto; el servicio de análisis espera un arreglo de textos. */
    @Column(columnDefinition = "TEXT", nullable = false)
    var temasIncorrectosJson: String
)
