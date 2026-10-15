package com.umbral.umbral_backend.dto

import jakarta.validation.constraints.NotBlank

data class AprobarRechazarDTO(
    @field:NotBlank
    val motivoRechazo: String
)
