package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.LocalDateTime

/** Agrupación de dudas por docente, sección y pregunta o tema. */
@Entity @Table(name="grupos_dudas")
class GrupoDudas(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) val id: Long = 0,
    /** Clave única que evita crear dos agrupaciones para el mismo criterio. */
    @Column(unique=true,nullable=false,length=64) val clave: String,
    val docenteId: Long,
    val seccionId: Long,
    val tema: String,
    val preguntaId: Long? = null
)

/** Duda del alumno con su agrupación, estado y respuesta asociada. */
@Entity @Table(name="dudas")
class Duda(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) val id: Long = 0,
    val grupoId: Long,
    val alumnoId: Long,
    @Column(length=4000,nullable=false) val texto: String,
    var estado: String = "PENDIENTE",
    /** Permite vincular varias dudas con una misma respuesta docente. */
    var respuestaId: Long? = null,
    val fecha: LocalDateTime = LocalDateTime.now()
)
