package com.umbral.app.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.umbral.app.domain.Rol
import com.umbral.app.domain.Usuario
import com.umbral.app.domain.UmbralRepository
import com.umbral.app.ui.common.EstadoVacio
import com.umbral.app.ui.theme.Luz
import com.umbral.app.ui.theme.Tinta

/** Pantalla de entrada: en la demo se elige un perfil; luego será un login con la API. */
@Composable
fun LoginScreen(repo: UmbralRepository, onEntrar: (Usuario) -> Unit) {
    var usuarios by remember { mutableStateOf<List<Usuario>>(emptyList()) }
    LaunchedEffect(Unit) { usuarios = repo.usuariosDemo() }

    Column(Modifier.fillMaxSize().background(Tinta).statusBarsPadding().padding(24.dp)) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(14.dp).background(Luz, CircleShape))
        Spacer(Modifier.height(16.dp))
        Text("Umbral", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "Tu academia en el bolsillo. Elige cómo quieres entrar.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB4BDD6),
        )
        Spacer(Modifier.height(28.dp))
        if (usuarios.isEmpty()) {
            EstadoVacio("Sin perfiles de demostración", "Revisa los datos locales de la app.")
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            usuarios.forEach { u -> TarjetaPerfil(u) { onEntrar(u) } }
        }
    }
}

@Composable
private fun TarjetaPerfil(usuario: Usuario, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(44.dp).background(colorDe(usuario.rol), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(usuario.nombre.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Entrar como ${usuario.rol.titulo.lowercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${usuario.nombre} · ${usuario.detalle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun colorDe(rol: Rol): Color = when (rol) {
    Rol.ALUMNO -> Color(0xFFCF3B18)
    Rol.DOCENTE -> Color(0xFF0E8F7A)
    Rol.APODERADO -> Color(0xFF101B3B)
}
