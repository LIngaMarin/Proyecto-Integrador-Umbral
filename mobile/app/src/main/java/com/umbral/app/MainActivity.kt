package com.umbral.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.umbral.app.ui.navegacion.UmbralApp
import com.umbral.app.ui.theme.UmbralTheme

/**
 * Unica Activity. Ya no crea el repositorio: las dependencias viven en AppContainer
 * (UmbralApplication) y la navegacion en UmbralApp.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UmbralTheme {
                UmbralApp()
            }
        }
    }
}
