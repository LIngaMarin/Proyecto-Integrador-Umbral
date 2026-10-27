package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

/** Estados disponibles para consultar y resolver dudas. */
enum class EstadoDuda { PENDIENTE, RESUELTA }

/** Evento de una duda que conserva la acción, el actor y la fecha. */
@Entity @Table(name="historial_dudas")
class HistorialDuda(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) val id: Long = 0,
    val dudaId: Long,
    val actorId: Long,
    val rol: String,
    val evento: String,
    val fecha: LocalDateTime = LocalDateTime.now()
)
