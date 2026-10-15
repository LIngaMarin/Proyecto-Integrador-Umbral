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

enum class EstadoSolucionario {
    BORRADOR,
    APROBADO
}

@Entity
@Table(name = "solucionarios")
class Solucionario(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @OneToOne
    @JoinColumn(name = "pregunta_id", nullable = false, unique = true)
    val pregunta: Pregunta,

    @Column(columnDefinition = "TEXT", nullable = false)
    var pasoAPaso: String,

    @Column(columnDefinition = "TEXT")
    var explicacionErroresComunes: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    var estado: EstadoSolucionario = EstadoSolucionario.BORRADOR
)
