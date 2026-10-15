package com.umbral.umbral_backend.repository

import com.umbral.umbral_backend.model.Pregunta
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PreguntaRepository : JpaRepository<Pregunta, Long>