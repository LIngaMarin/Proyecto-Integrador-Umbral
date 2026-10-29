package com.umbral.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.umbral.app.data.local.AlumnoSeccionEntity
import com.umbral.app.data.local.AppDatabase
import com.umbral.app.data.local.ClaseDocenteEntity
import com.umbral.app.data.local.ProgresoEntity
import com.umbral.app.data.local.TemaDebilEntity
import com.umbral.app.data.remote.RetrofitClient
import com.umbral.app.data.repository.SimulacroRepository
import com.umbral.app.model.Asistencia
import com.umbral.app.model.Pregunta
import com.umbral.app.ui.screens.attendance.AsistenciaQrScreen
import com.umbral.app.ui.screens.practice.ExplicacionIaScreen
import com.umbral.app.ui.screens.practice.PracticaSemanalScreen
import com.umbral.app.ui.screens.practice.PraticaTemasScreen
import com.umbral.app.ui.screens.practice.ResolverSimulacroContainer
import com.umbral.app.ui.screens.practice.TemasDebilesScreen
import com.umbral.app.ui.screens.progress.ComparativoAnonimoScreen
import com.umbral.app.ui.screens.progress.ComparativoDesempenoDto
import com.umbral.app.ui.screens.progress.ProgresoSemanalScreen
import com.umbral.app.ui.screens.teacher.HorarioDocenteScreen
import com.umbral.app.ui.screens.teacher.ListaAlumnosSeccionScreen
import com.umbral.app.ui.theme.UmbralTheme
import com.umbral.app.ui.viewmodel.SimulacroViewModel

enum class ScreenOption(val title: String) {
    PRACTICA_SEMANAL("Práctica Semanal"),
    PRATICA_TEMAS("Práctica por Temas"),
    RESOLVER_SIMULACRO("Simulacro"),
    EXPLICACION_IA("Explicación IA"),
    PROGRESO_SEMANAL("Progreso"),
    TEMAS_DEBILES("Temas Débiles"),
    COMPARATIVO_ANONIMO("Comparativo Anónimo"),
    ASISTENCIA_QR("Asistencia QR"),
    HORARIO_DOCENTE("Horario Docente"),
    LISTA_ALUMNOS("Lista Alumnos")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UmbralTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    var selectedScreen by remember { mutableStateOf(ScreenOption.PRACTICA_SEMANAL) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Umbral - Navegación de Pantallas") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedScreen.ordinal,
                    modifier = Modifier.fillMaxWidth(),
                    edgePadding = 16.dp
                ) {
                    ScreenOption.entries.forEach { screen ->
                        Tab(
                            selected = selectedScreen == screen,
                            onClick = { selectedScreen = screen },
                            text = { Text(screen.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        val modifier = Modifier.padding(innerPadding)

        when (selectedScreen) {
            ScreenOption.PRACTICA_SEMANAL -> {
                PracticaSemanalScreen(
                    preguntas = preguntasEjemplo,
                    modifier = modifier
                )
            }
            ScreenOption.PRATICA_TEMAS -> {
                PraticaTemasScreen(
                    temas = temasEjemplo,
                    onTemaSeleccionado = { },
                    modifier = modifier
                )
            }
            ScreenOption.RESOLVER_SIMULACRO -> {
                val context = LocalContext.current
                val viewModel = remember {
                    val db = AppDatabase.getDatabase(context)
                    val repo = SimulacroRepository(
                        simulacroDao = db.simulacroDao(),
                        preguntaPracticaDao = db.preguntaPracticaDao(),
                        respuestaTemporalDao = db.respuestaTemporalDao(),
                        apiService = RetrofitClient.simulacroApiService
                    )
                    SimulacroViewModel(repo)
                }

                LaunchedEffect(Unit) {
                    viewModel.cargarSimulacro(1L)
                }

                ResolverSimulacroContainer(
                    viewModel = viewModel,
                    onVolverInicio = { selectedScreen = ScreenOption.PRACTICA_SEMANAL },
                    modifier = modifier
                )
            }
            ScreenOption.EXPLICACION_IA -> {
                ExplicacionIaScreen(
                    enunciadoEjercicio = "¿Cuál es la derivada de f(x) = x² + 3x?",
                    respuestaAlumno = "2x",
                    respuestaCorrecta = "2x + 3",
                    explicacionTexto = "La derivada de x² es 2x y la derivada de 3x es 3. Por lo tanto, f'(x) = 2x + 3.",
                    cargandoIa = false,
                    onSolicitarExplicacion = { },
                    onVolver = { },
                    modifier = modifier
                )
            }
            ScreenOption.PROGRESO_SEMANAL -> {
                ProgresoSemanalScreen(
                    listaProgreso = progresoEjemplo,
                    modifier = modifier
                )
            }
            ScreenOption.TEMAS_DEBILES -> {
                TemasDebilesScreen(
                    temasDebiles = temasDebilesEjemplo,
                    onPracticarTema = { selectedScreen = ScreenOption.PRATICA_TEMAS },
                    modifier = modifier
                )
            }
            ScreenOption.COMPARATIVO_ANONIMO -> {
                ComparativoAnonimoScreen(
                    datosComparativos = comparativoEjemplo,
                    modifier = modifier
                )
            }
            ScreenOption.ASISTENCIA_QR -> {
                AsistenciaQrScreen(
                    asistencia = asistenciaEjemplo,
                    modifier = modifier
                )
            }
            ScreenOption.HORARIO_DOCENTE -> {
                HorarioDocenteScreen(
                    clasesHoy = horarioDocenteEjemplo,
                    diaActual = "Jueves",
                    modifier = modifier
                )
            }
            ScreenOption.LISTA_ALUMNOS -> {
                ListaAlumnosSeccionScreen(
                    alumnos = alumnosSeccionEjemplo,
                    nombreSeccion = "Sección A - Álgebra",
                    modifier = modifier
                )
            }
        }
    }
}

// Datos de prueba para previsualizar las pantallas
private val preguntasEjemplo = listOf(
    Pregunta(
        id = 1,
        enunciado = "¿Cuánto es 5 + 3?",
        opciones = listOf("6", "8", "10", "12"),
        respuestaCorrecta = 1,
        tema = "Aritmética"
    ),
    Pregunta(
        id = 2,
        enunciado = "¿Cuál es el resultado de 4 × 6?",
        opciones = listOf("10", "20", "24", "28"),
        respuestaCorrecta = 2,
        tema = "Multiplicación"
    ),
    Pregunta(
        id = 3,
        enunciado = "¿Cuánto es 15 - 7?",
        opciones = listOf("6", "7", "8", "9"),
        respuestaCorrecta = 2,
        tema = "Operaciones básicas"
    )
)

private val temasEjemplo = listOf(
    "Álgebra - Ecuaciones",
    "Geometría - Triángulos",
    "Trigonometría - Razones",
    "Aritmética - Razones y Proporciones",
    "Física - Cinemática"
)

private val progresoEjemplo = listOf(
    ProgresoEntity(1, "Semana 1", "Álgebra", 5, 5, "Completado"),
    ProgresoEntity(2, "Semana 2", "Geometría", 3, 5, "En curso"),
    ProgresoEntity(3, "Semana 3", "Trigonometría", 0, 5, "Pendiente")
)

private val temasDebilesEjemplo = listOf(
    TemaDebilEntity(
        id = 1,
        simulacroId = 1,
        tema = "Trigonometría - Identidades",
        porcentajeAcierto = 20f,
        preguntasIncorrectas = 4,
        preguntasTotales = 5,
        recomendacion = "Repasa las fórmulas de simplificación trigonométrica."
    ),
    TemaDebilEntity(
        id = 2,
        simulacroId = 1,
        tema = "Física - Dinámica",
        porcentajeAcierto = 40f,
        preguntasIncorrectas = 3,
        preguntasTotales = 5,
        recomendacion = "Refuerza la segunda Ley de Newton y diagramas de cuerpo libre."
    )
)

private val comparativoEjemplo = ComparativoDesempenoDto(
    miPuntajePromedio = 16.5f,
    promedioSeccion = 13.8f,
    mejorPuntajeSeccion = 19.5f,
    percentilAlumno = 88,
    totalAlumnosAnonimos = 35
)

private val asistenciaEjemplo = Asistencia(
    id = 1,
    alumnoNombre = "Juan Pérez",
    sede = "Sede Central",
    fechaHora = "15/10/2026 08:30 AM",
    estado = "Puntual",
    qrValidoHasta = "15/10/2026 09:00 AM"
)

private val horarioDocenteEjemplo = listOf(
    ClaseDocenteEntity(1, "Matemática I", "Aula 302", "08:00 AM", "10:00 AM", "Jueves"),
    ClaseDocenteEntity(2, "Álgebra Avanzada", "Aula 104", "10:30 AM", "12:30 PM", "Jueves")
)

private val alumnosSeccionEjemplo = listOf(
    AlumnoSeccionEntity(1, "SEC-A1", "Carlos Mendoza", "ALU-001"),
    AlumnoSeccionEntity(2, "SEC-A1", "María Flores", "ALU-002"),
    AlumnoSeccionEntity(3, "SEC-A1", "Luis Quispe", "ALU-003")
)

@Preview(showBackground = true)
@Composable
fun MainAppScreenPreview() {
    UmbralTheme {
        MainAppScreen()
    }
}
