package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

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
