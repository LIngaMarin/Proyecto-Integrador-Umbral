package com.umbral.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.umbral.app.data.demo.DemoRepository
import com.umbral.app.domain.Rol
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.domain.Usuario
import com.umbral.app.ui.alumno.AlumnoRoot
import com.umbral.app.ui.apoderado.ApoderadoRoot
import com.umbral.app.ui.docente.DocenteRoot
import com.umbral.app.ui.login.LoginScreen
import com.umbral.app.ui.theme.UmbralTheme

class MainActivity : ComponentActivity() {

    // Para conectar el backend: reemplazar DemoRepository por una implementación de UmbralRepository con la API.
    private val repo: UmbralRepository = DemoRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UmbralTheme {
                UmbralApp(repo)
            }
        }
    }
}

@Composable
private fun UmbralApp(repo: UmbralRepository) {
    var usuario by remember { mutableStateOf<Usuario?>(null) }
    val actual = usuario
    if (actual == null) {
        LoginScreen(repo = repo, onEntrar = { usuario = it })
    } else {
        val salir = { usuario = null }
        when (actual.rol) {
            Rol.ALUMNO -> AlumnoRoot(repo = repo, usuario = actual, onSalir = salir)
            Rol.DOCENTE -> DocenteRoot(repo = repo, usuario = actual, onSalir = salir)
            Rol.APODERADO -> ApoderadoRoot(repo = repo, usuario = actual, onSalir = salir)
        }
    }
}
