package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

/** Estados usados para registrar la revisión de una pregunta. */
enum class EstadoPregunta { PENDIENTE, APROBADA, RECHAZADA }
/** Valores de dificultad disponibles para clasificar preguntas. */
enum class Dificultad { FACIL, INTERMEDIO, DIFICIL }

/** Entidad persistida en la tabla preguntas con su contenido y datos de revisión. */
@Entity
@Table(name = "preguntas")
class Pregunta(
    /** Identificador generado por la base de datos. */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    /** Enunciado almacenado como texto largo. */
    @Column(columnDefinition = "TEXT")
    var enunciado: String,

    var tema: String,

    /** Se almacena el nombre del valor de dificultad. */
    @Enumerated(EnumType.STRING)
    var dificultad: Dificultad,

    /** Opciones persistidas como colección de elementos asociada a la entidad. */
    @ElementCollection
    var opciones: List<String> = mutableListOf(),

    var respuestaCorrecta: String,

    /** Estado de revisión persistido como texto. */
    @Enumerated(EnumType.STRING)
    var estado: EstadoPregunta = EstadoPregunta.PENDIENTE,

    var motivoRechazo: String? = null,

    val docenteId: Long,

    val fechaCreacion: LocalDateTime = LocalDateTime.now()
)
