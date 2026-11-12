package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

/** Respuesta docente persistida para atender un conjunto de dudas de la agrupación. */
@Entity @Table(name="respuestas_grupo")
class RespuestaGrupo(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) val id: Long = 0,
    val grupoId: Long,
    val docenteId: Long,
    @Column(length=10000,nullable=false) val texto: String,
    val fecha: LocalDateTime = LocalDateTime.now()
)
