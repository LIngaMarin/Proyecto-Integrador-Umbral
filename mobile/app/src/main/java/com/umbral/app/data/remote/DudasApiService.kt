package com.umbral.app.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

data class DudaResponseDto(
    val id: Long,
    val mensaje: String,
    val fotoUrl: String?,
    val estado: String
)

interface DudasApiService {
    @Multipart
    @POST("api/dudas/enviar-foto")
    suspend fun enviarDudaConFoto(
        @Part("alumnoId") alumnoId: RequestBody,
        @Part("preguntaId") preguntaId: RequestBody,
        @Part("comentario") comentario: RequestBody,
        @Part foto: MultipartBody.Part
    ): DudaResponseDto
}
