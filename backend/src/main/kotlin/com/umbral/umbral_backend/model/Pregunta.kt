package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class EstadoPregunta { PENDIENTE, APROBADA, RECHAZADA }
enum class Dificultad { FACIL, INTERMEDIO, DIFICIL }

@Entity
@Table(name = "preguntas")
class Pregunta(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(columnDefinition = "TEXT")
    var enunciado: String,

    var tema: String,

    @Enumerated(EnumType.STRING)
    var dificultad: Dificultad,

    @ElementCollection
    var opciones: List<String> = mutableListOf(),

    var respuestaCorrecta: String,

    @Enumerated(EnumType.STRING)
    var estado: EstadoPregunta = EstadoPregunta.PENDIENTE,

    var motivoRechazo: String? = null,

    val docenteId: Long,

    val fechaCreacion: LocalDateTime = LocalDateTime.now()
)