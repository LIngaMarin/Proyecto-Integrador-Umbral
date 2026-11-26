package com.umbral.app.domain

/**
 * Errores de la app, sin depender de Retrofit ni de Room.
 * El repositorio convierte IOException, HttpException, etc. en uno de estos,
 * asi los ViewModels y las pantallas nunca conocen la libreria de red.
 */
sealed class ErrorUmbral(mensaje: String) : Exception(mensaje) {
    class SinConexion : ErrorUmbral("Sin conexión a internet")
    class CredencialesInvalidas : ErrorUmbral("Correo o contraseña incorrectos")
    class SesionExpirada : ErrorUmbral("La sesión expiró")
    class Servidor(val codigo: Int) : ErrorUmbral("Error del servidor ($codigo)")
    class RespuestaInesperada : ErrorUmbral("Respuesta inesperada del servidor")
}
