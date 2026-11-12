package com.umbral.umbral_backend.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

/** Estados definidos para el ciclo de revisión del solucionario. */
enum class EstadoSolucionario {
    BORRADOR,
    APROBADO
}

/** Solución paso a paso asociada de forma uno a uno con una pregunta. */
@Entity
@Table(name = "solucionarios")
class Solucionario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** La restricción de unicidad limita la asociación a un solucionario por pregunta. */
    @OneToOne
    @JoinColumn(name = "pregunta_id", nullable = false, unique = true)
    val pregunta: Pregunta,

    /** Desarrollo escrito de los pasos de solución. */
    @Column(name = "paso_a_paso", columnDefinition = "TEXT", nullable = false)
    var pasoAPaso: String,

    /** Explicación opcional de errores comunes. */
    @Column(columnDefinition = "TEXT")
    var explicacionErroresComunes: String? = null,

    /** BORRADOR es el valor inicial; APROBADO es el estado al que lo cambia el servicio. */
    @Enumerated(EnumType.STRING)
    var estado: EstadoSolucionario = EstadoSolucionario.BORRADOR
)
