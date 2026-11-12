package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

/** Intento de práctica que conserva la opción elegida, su evaluación y la fecha. */
@Entity
@Table(name = "respuestas_practica")
class RespuestaPractica(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long = 0,
    val preguntaId: Long,
    val alumnoId: Long,
    val respuesta: String,
    val correcta: Boolean,
    val fecha: LocalDateTime = LocalDateTime.now()
)
