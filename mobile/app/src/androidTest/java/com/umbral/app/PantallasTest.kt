package com.umbral.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.umbral.app.ui.common.ErrorConReintento
import com.umbral.app.ui.login.LoginContenido
import com.umbral.app.ui.login.LoginUiState
import com.umbral.app.ui.theme.UmbralTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Pruebas de interfaz (se ejecutan en el emulador). */
@RunWith(AndroidJUnit4::class)
class PantallasTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun ingresarSeHabilitaSoloConCorreoYClave() {
        var estado by mutableStateOf(LoginUiState())
        compose.setContent {
            UmbralTheme {
                LoginContenido(
                    estado = estado,
                    cuentasDemo = emptyList(),
                    onCorreo = { estado = estado.copy(correo = it) },
                    onClave = { estado = estado.copy(clave = it) },
                    onIngresar = {},
                    onCuentaDemo = {},
                )
            }
        }
        compose.onNodeWithTag("botonIngresar").assertIsNotEnabled()
        compose.onNodeWithTag("campoCorreo").performTextInput("camila@umbral.pe")
        compose.onNodeWithTag("botonIngresar").assertIsNotEnabled()
        compose.onNodeWithTag("campoClave").performTextInput("umbral123")
        compose.onNodeWithTag("botonIngresar").assertIsEnabled()
    }

    @Test
    fun errorDeLoginSeMuestra() {
        compose.setContent {
            UmbralTheme {
                LoginContenido(
                    estado = LoginUiState(correo = "a@b.pe", clave = "x", error = "Correo o contraseña incorrectos."),
                    cuentasDemo = emptyList(),
                    onCorreo = {}, onClave = {}, onIngresar = {}, onCuentaDemo = {},
                )
            }
        }
        compose.onNodeWithText("Correo o contraseña incorrectos.").assertIsDisplayed()
    }

    @Test
    fun reintentarLlamaALaAccion() {
        var intentos = 0
        compose.setContent {
            UmbralTheme { ErrorConReintento("Sin conexión a internet.", onReintentar = { intentos++ }) }
        }
        compose.onNodeWithText("Sin conexión a internet.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, intentos)
    }
}
