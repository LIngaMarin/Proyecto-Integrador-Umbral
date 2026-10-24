package com.umbral.umbral_backend.dto

import jakarta.validation.constraints.NotBlank

/** Cuerpo de solicitud usado para aportar el motivo obligatorio del rechazo. */
data class AprobarRechazarDTO(
    /** El campo no puede estar vacío ni contener solo espacios, según @NotBlank. */
    @field:NotBlank
    val motivoRechazo: String
)
