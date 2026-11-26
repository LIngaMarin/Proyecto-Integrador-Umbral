package com.umbral.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val EsquemaClaro = lightColorScheme(
    primary = Primario,
    onPrimary = Superficie,
    primaryContainer = PrimarioContenedor,
    onPrimaryContainer = SobrePrimarioContenedor,
    secondary = Tinta,
    onSecondary = Superficie,
    secondaryContainer = TintaSuave,
    onSecondaryContainer = Tinta,
    tertiary = AcentoOscuro,
    onTertiary = Superficie,
    tertiaryContainer = AcentoContenedor,
    onTertiaryContainer = Tinta,
    background = Fondo,
    onBackground = TextoFuerte,
    surface = Superficie,
    onSurface = TextoFuerte,
    surfaceVariant = SuperficieVariante,
    onSurfaceVariant = TextoSuave,
    outline = TextoSuave,
    outlineVariant = Borde,
    error = ErrorRojo,
)

private val EsquemaOscuro = darkColorScheme(
    primary = PrimarioClaro,
    onPrimary = SobrePrimarioContenedor,
    primaryContainer = Primario,
    onPrimaryContainer = Superficie,
    secondary = TintaSuave,
    onSecondary = Tinta,
    tertiary = Acento,
    onTertiary = Tinta,
    background = FondoOscuro,
    onBackground = TextoOscuro,
    surface = SuperficieOscura,
    onSurface = TextoOscuro,
    surfaceVariant = Tinta,
    onSurfaceVariant = Borde,
)

/** Tema de Umbral. Sin color dinámico: la marca debe verse igual en todos los teléfonos. */
@Composable
fun UmbralTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) EsquemaOscuro else EsquemaClaro,
        typography = Typography,
        content = content,
    )
}
