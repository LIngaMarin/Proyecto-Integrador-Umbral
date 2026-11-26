package com.umbral.app.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.umbral.app.domain.ErrorUmbral
import com.umbral.app.ui.theme.ErrorRojo

/** Mensaje para el usuario segun el error que devolvio el repositorio. */
fun mensajeDe(e: Throwable): String = when (e) {
    is ErrorUmbral.SinConexion -> "Sin conexión a internet.\nRevisa tu red e inténtalo otra vez."
    is ErrorUmbral.CredencialesInvalidas -> "Correo o contraseña incorrectos."
    is ErrorUmbral.SesionExpirada -> "Tu sesión expiró. Vuelve a iniciar sesión."
    is ErrorUmbral.Servidor -> "Error del servidor (${e.codigo}). Inténtalo más tarde."
    is ErrorUmbral.RespuestaInesperada -> "El servidor respondió algo inesperado."
    else -> "Ocurrió un error inesperado."
}

/** Recuadro de error con su mensaje y el boton Reintentar. */
@Composable
fun ErrorConReintento(mensaje: String, onReintentar: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(ErrorRojo.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("errorConReintento"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            mensaje,
            color = ErrorRojo,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Button(onClick = onReintentar) { Text("Reintentar") }
    }
}

/** Pantalla completa de carga (por ejemplo, mientras se lee la sesion guardada). */
@Composable
fun PantallaCarga(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
