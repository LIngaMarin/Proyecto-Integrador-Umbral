package com.umbral.app.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega a TODAS las peticiones el encabezado Accept y, si hay sesion,
 * Authorization: Bearer <token> (el JWT que entrego Spring Boot en el login).
 */
class TokenInterceptor(private val tokenActual: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenActual()
        val peticion = chain.request().newBuilder()
            .header("Accept", "application/json")
            .apply { if (token != null) header("Authorization", "Bearer $token") }
            .build()
        return chain.proceed(peticion)
    }
}
