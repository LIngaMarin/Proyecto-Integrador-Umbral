package com.umbral.umbral_backend.dto

import jakarta.validation.constraints.*

data class RegistrarDudaDTO(
    @field:Positive val docenteId: Long,
    @field:Positive val seccionId: Long,
    @field:Positive val preguntaId: Long? = null,
    @field:Size(max=255) val tema: String? = null,
    @field:NotBlank @field:Size(max=4000) val texto: String
)
