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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.umbral.app.domain.CuentaDemo
import com.umbral.app.domain.Rol
import com.umbral.app.ui.common.Tarjeta
import com.umbral.app.ui.theme.ErrorRojo
import com.umbral.app.ui.theme.Luz
import com.umbral.app.ui.theme.Tinta

@Composable
fun LoginScreen(vm: LoginViewModel = viewModel(factory = LoginViewModel.Factory)) {
    val estado by vm.estado.collectAsStateWithLifecycle()
    LoginContenido(
        estado = estado,
        cuentasDemo = vm.cuentasDemo,
        onCorreo = vm::cambiarCorreo,
        onClave = vm::cambiarClave,
        onIngresar = vm::ingresar,
        onCuentaDemo = vm::entrarComo,
    )
}

/** Pantalla sin ViewModel (solo recibe estado y eventos): se puede probar con createComposeRule. */
@Composable
fun LoginContenido(
    estado: LoginUiState,
    cuentasDemo: List<CuentaDemo>,
    onCorreo: (String) -> Unit,
    onClave: (String) -> Unit,
    onIngresar: () -> Unit,
    onCuentaDemo: (CuentaDemo) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Tinta)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Spacer(Modifier.height(32.dp))
        Box(Modifier.size(14.dp).background(Luz, CircleShape))
        Spacer(Modifier.height(16.dp))
        Text("Umbral", style = MaterialTheme.typography.displaySmall, color = Color.White, fontWeight = FontWeight.Bold)
        Text(
            "Tu academia en el bolsillo. Ingresa con tu cuenta.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFB4BDD6),
        )
        Spacer(Modifier.height(24.dp))

        Tarjeta {
            OutlinedTextField(
                value = estado.correo,
                onValueChange = onCorreo,
                label = { Text("Correo") },
                singleLine = true,
                enabled = !estado.cargando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                modifier = Modifier.fillMaxWidth().testTag("campoCorreo"),
            )
            OutlinedTextField(
                value = estado.clave,
                onValueChange = onClave,
                label = { Text("Contraseña") },
                singleLine = true,
                enabled = !estado.cargando,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth().testTag("campoClave"),
            )
            val error = estado.error
            if (error != null) {
                Text(error, color = ErrorRojo, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            }
            Button(
                onClick = onIngresar,
                enabled = estado.puedeIngresar,
                modifier = Modifier.fillMaxWidth().testTag("botonIngresar"),
            ) {
                if (estado.cargando) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text("Ingresar")
                }
            }
        }

        if (cuentasDemo.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                "Modo demostración · entra rápido",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                cuentasDemo.forEach { c -> TarjetaPerfil(c, habilitada = !estado.cargando) { onCuentaDemo(c) } }
            }
        }
    }
}

@Composable
private fun TarjetaPerfil(cuenta: CuentaDemo, habilitada: Boolean, onClick: () -> Unit) {
    val usuario = cuenta.usuario
    Card(
        modifier = Modifier.fillMaxWidth().clickable(enabled = habilitada, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier.size(44.dp).background(colorDe(usuario.rol), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(usuario.nombre.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Entrar como ${usuario.rol.titulo.lowercase()}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "${usuario.nombre} · ${cuenta.correo}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun colorDe(rol: Rol): Color = when (rol) {
    Rol.ALUMNO -> Color(0xFFCF3B18)
    Rol.DOCENTE -> Color(0xFF0E8F7A)
    Rol.APODERADO -> Color(0xFF101B3B)
}
