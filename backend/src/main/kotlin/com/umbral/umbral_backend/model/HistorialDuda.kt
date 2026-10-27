package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

enum class EstadoDuda { PENDIENTE, RESUELTA }

@Entity @Table(name="historial_dudas")
class HistorialDuda(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) val id: Long = 0,
    val dudaId: Long,
    val actorId: Long,
    val rol: String,
    val evento: String,
    val fecha: LocalDateTime = LocalDateTime.now()
)
