package com.umbral.umbral_backend.model

import jakarta.persistence.*
import java.time.*

@Entity @Table(name="resumenes_semanales",uniqueConstraints=[UniqueConstraint(columnNames=["docente_id","semana_inicio"])])
class ResumenSemanal(
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) val id: Long = 0,
    val docenteId: Long,
    val semanaInicio: LocalDate,
    @Column(length=254,nullable=false) val destinatario: String,
    val asunto: String,
    @Column(columnDefinition="TEXT",nullable=false) val cuerpo: String,
    var estado: String = "PENDIENTE",
    var intentos: Int = 0,
    var ultimoError: String? = null,
    val creado: LocalDateTime = LocalDateTime.now(),
    var enviado: LocalDateTime? = null
)
