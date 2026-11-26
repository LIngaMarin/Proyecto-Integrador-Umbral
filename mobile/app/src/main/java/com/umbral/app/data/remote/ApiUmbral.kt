package com.umbral.app.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/** Arma Retrofit + OkHttp una sola vez (lo llama AppContainer). */
object ApiUmbral {

    fun crear(urlBase: String, tokenActual: () -> String?, depurar: Boolean): UmbralApi {
        // ignoreUnknownKeys: si Spring Boot agrega un campo nuevo, la app no se rompe
        val json = Json { ignoreUnknownKeys = true }

        val registro = HttpLoggingInterceptor().apply {
            level = if (depurar) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization") // el token no se imprime en Logcat
        }

        val cliente = OkHttpClient.Builder()
            .addInterceptor(TokenInterceptor(tokenActual))
            .addInterceptor(registro)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(urlBase)
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(UmbralApi::class.java)
    }
}
