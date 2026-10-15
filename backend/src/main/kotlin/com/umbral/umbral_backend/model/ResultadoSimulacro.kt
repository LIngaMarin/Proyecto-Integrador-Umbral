package com.umbral.umbral_backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "resultados_simulacro")
class ResultadoSimulacro(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    val simulacroId: Long,
    val alumnoId: Long,
    val seccionId: Long,
    var puntaje: Double,

    @Column(columnDefinition = "TEXT", nullable = false)
    var temasIncorrectosJson: String
)
